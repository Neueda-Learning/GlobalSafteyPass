import { useCallback, useEffect, useRef, useState } from 'react'
import {
  ArrowLeft, Bell, Check, ChevronRight, CircleAlert, CreditCard, FlaskConical,
  Home as HomeIcon, MapPin, Plane, Plus, RefreshCw, Route, ShieldCheck,
  Snowflake, UserRound, WalletCards, X
} from 'lucide-react'
import { api } from './api'

const money = (value = 0, currency = 'USD') =>
  new Intl.NumberFormat('en-US', { style: 'currency', currency }).format(value)
const shortDate = value => value
  ? new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric' }).format(new Date(`${value}T12:00:00`))
  : ''
const toDateInput = date => {
  const offset = date.getTimezoneOffset() * 60000
  return new Date(date.getTime() - offset).toISOString().slice(0, 10)
}
const nextDate = value => {
  const date = new Date(`${value}T12:00:00`)
  date.setDate(date.getDate() + 1)
  return toDateInput(date)
}
const destinationCurrencies = {
  australia: 'AUD', austria: 'EUR', belgium: 'EUR', brazil: 'BRL', canada: 'CAD',
  china: 'CNY', 'czech republic': 'CZK', czechia: 'CZK', denmark: 'DKK',
  finland: 'EUR', france: 'EUR', germany: 'EUR', greece: 'EUR', 'hong kong': 'HKD',
  hungary: 'HUF', india: 'INR', indonesia: 'IDR', ireland: 'EUR', italy: 'EUR',
  japan: 'JPY', malaysia: 'MYR', mexico: 'MXN', netherlands: 'EUR',
  'new zealand': 'NZD', norway: 'NOK', philippines: 'PHP', poland: 'PLN',
  portugal: 'EUR', singapore: 'SGD', 'south africa': 'ZAR', 'south korea': 'KRW',
  spain: 'EUR', sweden: 'SEK', switzerland: 'CHF', taiwan: 'TWD', thailand: 'THB',
  turkey: 'TRY', 'united arab emirates': 'AED', uae: 'AED',
  'united kingdom': 'GBP', uk: 'GBP', 'united states': 'USD', usa: 'USD',
  vietnam: 'VND'
}
const cityCurrencies = {
  tokyo: 'JPY', osaka: 'JPY', kyoto: 'JPY', singapore: 'SGD', london: 'GBP',
  paris: 'EUR', rome: 'EUR', berlin: 'EUR', madrid: 'EUR', seoul: 'KRW',
  bangkok: 'THB', sydney: 'AUD', melbourne: 'AUD', toronto: 'CAD',
  vancouver: 'CAD', dubai: 'AED', 'hong kong': 'HKD', taipei: 'TWD'
}
const normalized = value => value.trim().toLowerCase().replace(/\s+/g, ' ')
const currencyForDestination = (country, city) =>
  destinationCurrencies[normalized(country)] || cityCurrencies[normalized(city)] || ''
const supportedCurrencies = [...new Set([
  'USD', 'EUR', 'GBP', ...Object.values(destinationCurrencies), ...Object.values(cityCurrencies)
])].sort()
const nav = [
  ['home', 'Home', HomeIcon], ['trips', 'Trips', Route],
  ['activity', 'Activity', WalletCards], ['alerts', 'Alerts', Bell],
  ['profile', 'Profile', UserRound]
]

function App() {
  const screenRef = useRef(null)
  const [tab, setTab] = useState('home')
  const [subpage, setSubpage] = useState(null)
  const [data, setData] = useState({ dashboard: null, trips: [], cards: [], transactions: [], alerts: [] })
  const [loading, setLoading] = useState(true)
  const [toast, setToast] = useState('')
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    try {
      setError('')
      const [dashboard, trips, cards, transactions, alerts] = await Promise.all([
        api.dashboard(), api.trips(), api.cards(), api.transactions(), api.alerts()
      ])
      setData({ dashboard, trips, cards, transactions, alerts })
    } catch (e) {
      setError(e.message)
    } finally { setLoading(false) }
  }, [])

  useEffect(() => { load() }, [load])
  useEffect(() => {
    if (screenRef.current) screenRef.current.scrollTop = 0
  }, [tab, subpage])
  const notify = message => {
    setToast(message)
    window.setTimeout(() => setToast(''), 2600)
  }
  const mutate = async (task, message) => {
    try {
      setError('')
      await task()
      await load()
      notify(message)
      return true
    } catch (e) {
      setError(e.message)
      return false
    }
  }
  const navigate = (nextTab, nextSubpage = null) => {
    setTab(nextTab); setSubpage(nextSubpage); setError('')
  }

  if (loading) return <Shell><Loading /></Shell>

  let content
  if (subpage === 'create-trip') {
    content = <CreateTrip cards={data.cards} onBack={() => setSubpage(null)}
      onSubmit={payload => mutate(() => api.createTrip(payload), 'Trip created successfully.')} />
  } else if (subpage === 'readiness') {
    content = <Readiness trip={data.dashboard?.trip} result={data.dashboard?.readiness}
      onBack={() => setSubpage(null)}
      onRun={() => mutate(() => api.runReadiness(data.dashboard.trip.id), 'Readiness check updated.')} />
  } else if (subpage === 'demo') {
    content = <DemoLab cards={data.cards} trip={data.dashboard?.trip}
      onBack={() => setSubpage(null)}
      onSubmit={payload => mutate(() => api.sendPayment(payload), 'Bank event processed.')} />
  } else {
    content = {
      home: <Home data={data.dashboard} onGo={navigate} />,
      trips: <Trips trips={data.trips} readiness={data.dashboard?.readiness}
        onCreate={() => setSubpage('create-trip')} onReadiness={() => setSubpage('readiness')} />,
      activity: <Activity transactions={data.transactions} />,
      alerts: <Alerts alerts={data.alerts}
        onAction={(id, action) => mutate(() => api.alertAction(id, { action }), `Alert marked as ${action.toLowerCase()}.`)} />,
      profile: <Profile cards={data.cards}
        onUpdate={(id, patch, message) => mutate(() => api.updateCard(id, patch), message)}
        onDemo={() => setSubpage('demo')} />
    }[tab]
  }

  return (
    <Shell>
      <main className="screen" ref={screenRef}>
        {error && <div className="error-banner"><CircleAlert size={17}/><span>{error}</span><button onClick={() => setError('')}><X size={16}/></button></div>}
        {content}
      </main>
      {!subpage && <BottomNav active={tab} alerts={data.dashboard?.openAlertCount}
        onSelect={value => navigate(value)} />}
      {toast && <div className="toast"><Check size={17}/>{toast}</div>}
    </Shell>
  )
}

function Shell({ children }) {
  return <div className="app-stage"><div className="phone" onScroll={event => {
    if (event.currentTarget.scrollTop || event.currentTarget.scrollLeft) {
      event.currentTarget.scrollTo(0, 0)
    }
  }}>{children}</div></div>
}

function Header({ eyebrow, title, action, onBack }) {
  return <header className="page-header">
    {onBack && <button className="icon-button" onClick={onBack} aria-label="Go back"><ArrowLeft size={20}/></button>}
    <div className="header-copy">{eyebrow && <span>{eyebrow}</span>}<h1>{title}</h1></div>
    {action || <span className="header-space"/>}
  </header>
}

function Home({ data, onGo }) {
  if (!data?.trip) return <Empty title="Plan your first trip" copy="Create a trip to check card readiness and monitor activity."/>
  const { user, trip, readiness, recentTransactions, openAlertCount } = data
  const percentage = Math.min(100, Math.round((Number(trip.spent) / Number(trip.budget)) * 100))
  return <>
    <Header eyebrow="Good morning" title={user.name.split(' ')[0]}
      action={<button className="avatar" aria-label="Profile" onClick={() => onGo('profile')}>AM</button>} />
    <button className="trip-hero" onClick={() => onGo('trips')}>
      <div className="hero-top"><span className="chip light"><Plane size={14}/> Active trip</span><ChevronRight size={19}/></div>
      <div><MapPin size={18}/><h2>{trip.destinationCity}</h2></div>
      <p>{trip.destinationCountry} · {shortDate(trip.startDate)}–{shortDate(trip.endDate)}</p>
    </button>
    <section className="readiness-card">
      <div><span className="section-kicker">Travel readiness</span><h3>{readiness.status}</h3>
        <p>{readiness.score >= 80 ? "You're ready to travel." : 'A few card settings need attention.'}</p>
      </div>
      <button className={`score ${readiness.score < 80 ? 'warn' : ''}`} onClick={() => onGo('home', 'readiness')}>
        <strong>{readiness.score}</strong><span>/100</span>
      </button>
    </section>
    <section className="budget-card">
      <div className="budget-head"><div><span>Trip spend</span><strong>{money(trip.spent, trip.currency)}</strong></div>
        <div className="right"><span>Remaining</span><strong>{money(trip.remaining, trip.currency)}</strong></div></div>
      <div className="progress"><i style={{ width: `${percentage}%` }}/></div>
      <small>{percentage}% of {money(trip.budget, trip.currency)} budget</small>
    </section>
    {openAlertCount > 0 && <button className="alert-callout" onClick={() => onGo('alerts')}>
      <span className="alert-icon"><Bell size={19}/></span>
      <span><strong>{openAlertCount} security {openAlertCount === 1 ? 'alert' : 'alerts'}</strong><small>Needs your attention</small></span>
      <ChevronRight size={18}/>
    </button>}
    <SectionTitle title="Recent activity" action="View all" onClick={() => onGo('activity')}/>
    <div className="list">{recentTransactions.map(tx => <TransactionRow key={tx.id} tx={tx}/>)}</div>
  </>
}

function Trips({ trips, readiness, onCreate, onReadiness }) {
  return <>
    <Header eyebrow="Travel plans" title="Your trips"
      action={<button className="icon-button filled" onClick={onCreate} aria-label="Create trip"><Plus size={20}/></button>} />
    {trips.map((trip, index) => <article className="trip-card" key={trip.id}>
      <div className="trip-image"><Plane size={25}/></div>
      <div className="trip-card-body">
        <span className="chip">{index === 0 ? 'Active' : 'Planned'}</span>
        <h2>{trip.destinationCity}</h2><p>{trip.destinationCountry}</p>
        <div className="meta">{shortDate(trip.startDate)}–{shortDate(trip.endDate)} · {money(trip.budget, trip.currency)}</div>
        {index === 0 && <button className="inline-link" onClick={onReadiness}>
          <ShieldCheck size={17}/>{readiness?.score || 0}% ready <ChevronRight size={16}/>
        </button>}
      </div>
    </article>)}
  </>
}

function CreateTrip({ cards, onBack, onSubmit }) {
  const inThreeDays = toDateInput(new Date(Date.now() + 86400000 * 3))
  const inTenDays = toDateInput(new Date(Date.now() + 86400000 * 10))
  const [form, setForm] = useState({
    destinationCountry: 'Japan', destinationCity: 'Tokyo', startDate: inThreeDays,
    endDate: inTenDays, budget: '2500', currency: 'JPY', cardId: cards[0]?.id || ''
  })
  const [currencyAuto, setCurrencyAuto] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const field = (key, value) => setForm(f => ({ ...f, [key]: value }))
  const destinationField = (key, value) => {
    setCurrencyAuto(true)
    setForm(current => {
      const next = { ...current, [key]: value }
      const matchedCurrency = currencyForDestination(next.destinationCountry, next.destinationCity)
      return matchedCurrency ? { ...next, currency: matchedCurrency } : next
    })
  }
  const startDate = value => setForm(current => ({
    ...current,
    startDate: value,
    endDate: !value || (current.endDate && current.endDate > value) ? current.endDate : nextDate(value)
  }))
  const dateError = form.startDate && form.endDate && form.endDate <= form.startDate
    ? 'Return date must be after the departure date.'
    : ''
  const matchedCurrency = currencyForDestination(form.destinationCountry, form.destinationCity)
  const submit = async event => {
    event.preventDefault()
    if (dateError || submitting || !cards.length) return
    setSubmitting(true)
    const succeeded = await onSubmit({
      ...form,
      destinationCountry: form.destinationCountry.trim(),
      destinationCity: form.destinationCity.trim(),
      budget: Number(form.budget),
      cardId: Number(form.cardId)
    })
    setSubmitting(false)
    if (succeeded) onBack()
  }
  return <>
    <Header title="Create a trip" onBack={onBack}/>
    <p className="intro">Tell us where you're going. We'll check that your card is travel-ready.</p>
    <form className="form" onSubmit={submit}>
      <label>Country<input value={form.destinationCountry} onChange={e => destinationField('destinationCountry', e.target.value)} autoComplete="country-name" required/></label>
      <label>City<input value={form.destinationCity} onChange={e => destinationField('destinationCity', e.target.value)} autoComplete="address-level2" required/></label>
      <div className="field-pair">
        <label>Departure<input type="date" value={form.startDate} onChange={e => startDate(e.target.value)} required/></label>
        <label>Return<input type="date" value={form.endDate} min={form.startDate ? nextDate(form.startDate) : undefined}
          aria-invalid={Boolean(dateError)} onChange={e => field('endDate', e.target.value)} required/></label>
      </div>
      {dateError && <p className="field-message error" role="alert">{dateError}</p>}
      <div className="field-pair compact">
        <label>Budget<input type="number" min="1" step=".01" inputMode="decimal" value={form.budget} onChange={e => field('budget', e.target.value)} required/></label>
        <label>Currency<select value={form.currency} onChange={e => { field('currency', e.target.value); setCurrencyAuto(false) }}>
          {supportedCurrencies.map(currency => <option key={currency}>{currency}</option>)}
        </select></label>
      </div>
      <p className="field-message">{currencyAuto && matchedCurrency
        ? `${matchedCurrency} selected automatically for this destination.`
        : matchedCurrency
          ? `Suggested currency: ${matchedCurrency}. Your manual selection is kept.`
          : 'Currency could not be detected. Please choose it manually.'}</p>
      <label>Preferred card<select value={form.cardId} onChange={e => field('cardId', e.target.value)} required disabled={!cards.length}>
        {cards.map(card => <option key={card.id} value={card.id}>{card.nickname} ···· {card.lastFour}</option>)}
      </select></label>
      {!cards.length && <p className="field-message error" role="alert">Add a card before creating a trip.</p>}
      <button className="primary" type="submit" disabled={Boolean(dateError) || submitting || !cards.length}>
        {submitting ? 'Creating trip…' : 'Create trip & check readiness'}
      </button>
    </form>
  </>
}

function Readiness({ trip, result, onBack, onRun }) {
  if (!trip || !result) return <Empty title="No active trip" copy="Create a trip to run a readiness check."/>
  return <>
    <Header title="Travel readiness" onBack={onBack}
      action={<button className="icon-button" onClick={onRun} aria-label="Refresh readiness"><RefreshCw size={19}/></button>}/>
    <div className="readiness-hero">
      <div className={`score large ${result.score < 80 ? 'warn' : ''}`}><strong>{result.score}</strong><span>/100</span></div>
      <h2>{result.status}</h2><p>{trip.destinationCity} · {shortDate(trip.startDate)}–{shortDate(trip.endDate)}</p>
    </div>
    <SectionTitle title="Your checklist"/>
    <div className="check-list">{result.items.map(item => <div className="check-item" key={item.key}>
      <span className={item.passed ? 'pass' : 'fail'}>{item.passed ? <Check size={17}/> : <CircleAlert size={17}/>}</span>
      <div><strong>{item.label}</strong><p>{item.message}</p></div>
      <span className="points">+{item.passed ? item.points : 0}</span>
    </div>)}</div>
    <button className="primary" onClick={onRun}><RefreshCw size={18}/> Run check again</button>
  </>
}

function Activity({ transactions }) {
  const [filter, setFilter] = useState('ALL')
  const visible = transactions.filter(tx => filter === 'ALL' || tx.status === filter || (filter === 'UNMATCHED' && !tx.trip))
  return <>
    <Header eyebrow="Synced from your bank" title="Activity"/>
    <div className="segmented">
      {['ALL', 'APPROVED', 'DECLINED', 'UNMATCHED'].map(item =>
        <button key={item} className={filter === item ? 'active' : ''} onClick={() => setFilter(item)}>
          {item[0] + item.slice(1).toLowerCase()}</button>)}
    </div>
    <p className="sync-note"><RefreshCw size={14}/> Transactions update automatically</p>
    <div className="list roomy">{visible.map(tx => <TransactionRow key={tx.id} tx={tx} detailed/>)}</div>
    {!visible.length && <Empty title="Nothing here yet" copy="New bank activity will appear automatically."/>}
  </>
}

function TransactionRow({ tx, detailed }) {
  const approved = tx.status === 'APPROVED'
  return <article className="transaction-row">
    <span className={`merchant-icon ${approved ? '' : 'declined'}`}>{tx.category?.[0] || 'P'}</span>
    <div className="transaction-copy"><strong>{tx.merchant}</strong>
      <span>{tx.city} · {tx.category}{!tx.trip && ' · Unmatched'}</span>
      {detailed && !approved && <small>{tx.declineMessage} {tx.recommendedAction}</small>}
    </div>
    <div className="transaction-amount"><strong className={!approved ? 'negative' : ''}>
      {approved ? '−' : ''}{money(tx.amount, tx.currency)}</strong>
      <span>{approved ? 'Approved' : 'Declined'}</span></div>
  </article>
}

function Alerts({ alerts, onAction }) {
  const [selected, setSelected] = useState(null)
  const [actionPending, setActionPending] = useState('')
  const handleAction = async action => {
    if (!selected || actionPending) return
    setActionPending(action)
    const succeeded = await onAction(selected.id, action)
    setActionPending('')
    if (succeeded) setSelected(null)
  }
  return <>
    <Header eyebrow="Card protection" title="Security alerts"/>
    <div className="list roomy">{alerts.map(alert => <button className="alert-row" key={alert.id} onClick={() => setSelected(alert)}>
      <span className={`risk-dot ${alert.severity.toLowerCase()}`}><CircleAlert size={19}/></span>
      <span><small>{alert.severity} RISK · {alert.status.replaceAll('_', ' ')}</small><strong>{alert.title}</strong>
        <p>{alert.transaction.merchant} · {money(alert.transaction.amount, alert.transaction.currency)}</p></span>
      <ChevronRight size={18}/>
    </button>)}</div>
    {!alerts.length && <Empty title="You're all clear" copy="We'll let you know when something needs attention."/>}
    {selected && <div className="sheet-backdrop" onClick={() => setSelected(null)}>
      <section className="sheet" onClick={e => e.stopPropagation()}>
        <button className="sheet-close" onClick={() => setSelected(null)}><X size={19}/></button>
        <span className={`risk-dot large-risk ${selected.severity.toLowerCase()}`}><CircleAlert size={23}/></span>
        <span className="section-kicker">{selected.severity} risk</span><h2>{selected.title}</h2>
        <p>{selected.reason}</p>
        <div className="alert-facts"><span>Merchant<strong>{selected.transaction.merchant}</strong></span>
          <span>Amount<strong>{money(selected.transaction.amount, selected.transaction.currency)}</strong></span>
          <span>Location<strong>{selected.transaction.city}, {selected.transaction.country}</strong></span></div>
        {selected.status === 'OPEN' ? <div className="sheet-actions">
          <button className="primary" disabled={Boolean(actionPending)} onClick={() => handleAction('CONFIRM')}><Check size={18}/>
            {actionPending === 'CONFIRM' ? 'Confirming…' : 'This was me'}</button>
          <button className="secondary danger" disabled={Boolean(actionPending)}
            onClick={() => { if (confirm('Report this transaction as suspicious?')) handleAction('REPORT') }}>
            {actionPending === 'REPORT' ? 'Reporting…' : 'Report transaction'}</button>
          <button className="text-button" disabled={Boolean(actionPending)}
            onClick={() => { if (confirm('Freeze this card now?')) handleAction('FREEZE') }}><Snowflake size={17}/>
            {actionPending === 'FREEZE' ? 'Freezing…' : 'Freeze card'}</button>
        </div> : <div className="resolved"><Check size={18}/> This alert has been handled.</div>}
      </section>
    </div>}
  </>
}

function Profile({ cards, onUpdate, onDemo }) {
  const [pendingControl, setPendingControl] = useState('')
  const update = async (cardId, patch, message, control) => {
    if (pendingControl) return
    setPendingControl(`${cardId}-${control}`)
    await onUpdate(cardId, patch, message)
    setPendingControl('')
  }
  return <>
    <Header eyebrow="Alex Morgan" title="My cards"/>
    {cards.map(card => <article className={`bank-card ${card.frozen ? 'frozen' : ''}`} key={card.id}>
      <div className="card-top"><span>{card.nickname}</span><strong>{card.network}</strong></div>
      <div className="card-number">•••• &nbsp;•••• &nbsp;•••• &nbsp;{card.lastFour}</div>
      <div className="card-bottom"><span>Balance<strong>{money(card.balance, card.currency)}</strong></span>
        <span>Valid thru<strong>{card.expiry}</strong></span></div>
      {card.frozen && <span className="frozen-label"><Snowflake size={14}/> Frozen</span>}
    </article>)}
    <SectionTitle title="Card controls"/>
    {cards.map(card => <section className="control-card" key={card.id}>
      <div className="control-title"><CreditCard size={18}/><strong>•••• {card.lastFour}</strong></div>
      <label className="switch-row"><span><strong>Overseas payments</strong><small>Allow payments outside your home country</small></span>
        <input type="checkbox" checked={card.overseasEnabled} disabled={Boolean(pendingControl)}
          onChange={e => update(card.id, { overseasEnabled: e.target.checked }, 'Overseas payment setting updated.', 'overseas')}/><i/></label>
      <label className="switch-row"><span><strong>Freeze card</strong><small>Block all new payment attempts</small></span>
        <input type="checkbox" checked={card.frozen} disabled={Boolean(pendingControl)}
          onChange={e => update(card.id, { frozen: e.target.checked }, e.target.checked ? 'Card frozen.' : 'Card unfrozen.', 'frozen')}/><i/></label>
    </section>)}
    <button className="demo-link" onClick={onDemo}><FlaskConical size={19}/><span><strong>Open Demo Lab</strong><small>Simulate an incoming bank payment</small></span><ChevronRight size={18}/></button>
  </>
}

function DemoLab({ cards, trip, onBack, onSubmit }) {
  const defaultCard = cards[0]
  const [form, setForm] = useState({
    cardId: defaultCard?.id || '', merchant: 'City Market', amount: '42.50',
    country: trip?.destinationCountry || 'Singapore', city: trip?.destinationCity || 'Singapore',
    category: 'Shopping', channel: 'CONTACTLESS',
    currency: trip?.currency || defaultCard?.currency || 'USD'
  })
  const [submitting, setSubmitting] = useState(false)
  const field = (key, value) => setForm(f => ({ ...f, [key]: value }))
  const submit = async e => {
    e.preventDefault()
    if (submitting || !cards.length) return
    setSubmitting(true)
    const id = crypto.randomUUID()
    await onSubmit({
      ...form, cardId: Number(form.cardId), amount: Number(form.amount),
      provider: 'DEMO_BANK', externalTransactionId: id, eventId: `event-${id}`,
      exchangeRate: 1, occurredAt: new Date().toISOString().slice(0, 19)
    })
    setSubmitting(false)
  }
  return <>
    <Header title="Demo Lab" onBack={onBack}/>
    <div className="lab-note"><FlaskConical size={20}/><div><strong>Testing surface</strong><p>This simulates an automatic bank event. It is not manual expense entry.</p></div></div>
    <form className="form" onSubmit={submit}>
      <label>Card<select value={form.cardId} onChange={e => field('cardId', e.target.value)}>
        {cards.map(card => <option key={card.id} value={card.id}>{card.nickname} ···· {card.lastFour}</option>)}</select></label>
      <label>Merchant<input value={form.merchant} onChange={e => field('merchant', e.target.value)} required/></label>
      <div className="field-pair compact">
        <label>Amount<input type="number" min=".01" step=".01" inputMode="decimal" value={form.amount} onChange={e => field('amount', e.target.value)} required/></label>
        <label>Currency<select value={form.currency} onChange={e => field('currency', e.target.value)}>
          {supportedCurrencies.map(currency => <option key={currency}>{currency}</option>)}
        </select></label>
      </div>
      <div className="field-pair">
        <label>Country<input value={form.country} onChange={e => field('country', e.target.value)} required/></label>
        <label>City<input value={form.city} onChange={e => field('city', e.target.value)} required/></label>
      </div>
      <div className="field-pair">
        <label>Category<select value={form.category} onChange={e => field('category', e.target.value)}>
          {['Dining','Accommodation','Transport','Shopping','Entertainment','Cash','Other'].map(x => <option key={x}>{x}</option>)}</select></label>
        <label>Channel<select value={form.channel} onChange={e => field('channel', e.target.value)}>
          {['CONTACTLESS','POS','ONLINE','ATM'].map(x => <option key={x}>{x}</option>)}</select></label>
      </div>
      <button className="primary" type="submit" disabled={submitting || !cards.length}><CreditCard size={18}/>
        {submitting ? 'Sending event…' : 'Send bank event'}</button>
    </form>
  </>
}

function BottomNav({ active, alerts, onSelect }) {
  return <nav className="bottom-nav">{nav.map(([id, label, Icon]) =>
    <button key={id} className={active === id ? 'active' : ''} onClick={() => onSelect(id)}>
      <span><Icon size={21}/>{id === 'alerts' && alerts > 0 && <i>{alerts}</i>}</span><small>{label}</small>
    </button>)}</nav>
}

function SectionTitle({ title, action, onClick }) {
  return <div className="section-title"><h3>{title}</h3>{action && <button onClick={onClick}>{action}</button>}</div>
}
function Empty({ title, copy }) { return <div className="empty"><ShieldCheck size={34}/><h2>{title}</h2><p>{copy}</p></div> }
function Loading() { return <div className="loading"><div className="brand-mark"><ShieldCheck size={26}/></div><strong>Global Safety Pass</strong><span>Securing your journey…</span></div> }

export default App
