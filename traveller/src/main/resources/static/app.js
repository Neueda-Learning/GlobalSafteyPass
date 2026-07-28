let authToken=localStorage.getItem("traveller_access_token")||"";
const $=s=>document.querySelector(s);
const formatMoney=(n,currency="USD")=>{
  const safeCurrency=(currency||"USD").toUpperCase();
  try{return new Intl.NumberFormat("en-US",{style:"currency",currency:safeCurrency}).format(n||0)}
  catch{return new Intl.NumberFormat("en-US",{style:"currency",currency:"USD"}).format(n||0)}
};
const money=(n,currency)=>(formatMoney(n,currency||state.dashboard?.currency||"USD"));
const currencyMoney=(n,currency="USD")=>formatMoney(n,currency);
const dayCount=date=>Math.ceil((new Date(`${date}T12:00:00`)-new Date())/86400000);
let state={trips:[],dashboard:null,alerts:[],cards:[],transactions:[],cases:[]};
let activeRecovery=null;

async function api(path,opt={}){
  const headers={"Content-Type":"application/json",...opt.headers};
  if(authToken)headers.Authorization=`Bearer ${authToken}`;
  const r=await fetch(path,{...opt,headers});
  if(!r.ok){const e=await r.json().catch(()=>({message:"Request failed"}));if(r.status===401&&path.startsWith("/api/travel"))showAuth();throw Error(e.message)}
  return r.status===204?null:r.json();
}
function toast(text){const e=$("#toast");e.textContent=text;e.classList.add("show");setTimeout(()=>e.classList.remove("show"),2400)}
function openSheet(html){
  $("#homePage").classList.add("hidden");$("#contentPage").classList.remove("hidden");
  $("#contentPage").innerHTML=html;$("#sheet").classList.add("hidden");
  $("#contentPage").scrollTo({top:0,behavior:"smooth"});
}
const journeyCard=t=>`<div class="menu-card" onclick="showJourney('${t.id}')" role="button"><h3>${t.destinationCity||t.destinationCountry}, ${t.destinationCountry}</h3><p>${t.startDate} — ${t.endDate} · ${t.status}</p><div class="amount">${currencyMoney(t.budget,t.budgetCurrency)}</div><button class="inline-link">Open journey →</button></div>`;
const fraudReasonText={
  OUTSIDE_TRIP_DATE:"Outside your registered travel dates",OUTSIDE_DESTINATION:"Different from your trip destination",
  UNEXPECTED_CURRENCY:"Unusual currency for this destination",DUPLICATE_TRANSACTION:"Similar payment seen recently",
  HIGH_VALUE_TRANSACTION:"Higher than your usual spending",UNUSUAL_ATM_WITHDRAWAL:"Several cash withdrawals close together",
  RAPID_COUNTRY_CHANGE:"Used in different countries too quickly",MULTIPLE_DECLINES:"Several recent payment attempts",
  DECLINE_THEN_APPROVAL:"Large payment after recent declines",NEW_MERCHANT_CATEGORY:"A new type of merchant for you",
  CARD_FROZEN_TRANSACTION:"Activity on a frozen card",HIGH_RISK_COUNTRY:"Higher-risk payment location",
  CUSTOMER_PROFILE_ANOMALY:"Different from your normal spending pattern"
};
const friendlyFraudReasons=a=>(a.reasonCodes||[]).map(code=>fraudReasonText[code]||code.replaceAll("_"," ").toLowerCase());
const alertPayment=a=>`<div class="alert-payment"><strong>${currencyMoney(a.amount,a.currency)}</strong><b>${a.merchantName}</b>
  <small>${a.maskedCardNumber} · ${a.merchantCountry||"Location unavailable"}<br>${a.transactionTime?new Date(a.transactionTime).toLocaleString():"Time unavailable"}</small></div>`;
const alertCard=a=>{
  const reasons=friendlyFraudReasons(a),status=a.status;
  if(status==="OPEN")return `<article class="fraud-alert-card alert-open">
    <div class="alert-status"><div><small>ACTION NEEDED</small><h3>Is this your payment?</h3></div><span>${a.riskLevel} RISK</span></div>
    ${alertPayment(a)}
    <p>We paused to let you check the merchant, amount, location and time.</p>
    <div class="reason-summary"><b>Why we’re checking</b>${reasons.slice(0,3).map(r=>`<span>• ${r}</span>`).join("")}</div>
    <div class="menu-actions alert-actions"><button onclick="resolveAlert('${a.id}','confirm')">Yes, it was me</button><button class="danger" onclick="resolveAlert('${a.id}','report')">No, report fraud</button><button class="light" onclick="resolveAlert('${a.id}','freeze-card')">Freeze card</button></div>
  </article>`;
  if(status==="CONFIRMED_SAFE")return `<article class="fraud-alert-card alert-safe">
    <div class="resolved-alert-head"><div class="resolved-check">✓</div><div><small>REVIEW COMPLETE</small><h3>Confirmed as yours</h3></div></div>
    <div class="resolved-payment"><div><b>${a.merchantName}</b><small>${a.maskedCardNumber} · ${a.merchantCountry||"Location unavailable"}</small></div><strong>${currencyMoney(a.amount,a.currency)}</strong></div>
    <p>No further action is needed. Your card remains available to use.</p>
    ${reasons.length?`<details class="alert-explanation"><summary>Why we originally checked</summary><span>${reasons.join(" · ")}</span></details>`:""}
  </article>`;
  return `<article class="fraud-alert-card alert-incident">
    <div class="alert-status"><div><small>SECURITY CASE</small><h3>${status==="REPORTED_FRAUD"?"Reported as fraud":"Card frozen"}</h3></div><span>${status.replaceAll("_"," ")}</span></div>
    ${alertPayment(a)}
    <p>${status==="REPORTED_FRAUD"?"We’re investigating this payment. Follow the case for updates.":"This card cannot be used until you securely unfreeze it."}</p>
    ${a.caseReference?`<div class="case-reference"><small>FRAUD CASE</small><b>${a.caseReference}</b><span>Keep this reference for support.</span></div>`:""}
  </article>`;
};
function cardOptions(){
  return state.cards.filter(c=>c.status==="ACTIVE").map(c=>`<option value="${c.id}">${c.cardType} ${c.maskedCardNumber} · main ${c.mainCurrency||"—"} · supports ${(c.supportedCurrencies||[]).join("/")||"none"}${c.overseasPaymentsEnabled?"":" · overseas off"}</option>`).join("");
}
function openCreateTrip(){
  const start=new Date(Date.now()+7*86400000).toISOString().slice(0,10);
  const end=new Date(Date.now()+14*86400000).toISOString().slice(0,10);
  openSheet(`<button class="back" onclick="activate('trips')">← My journeys</button>
    <span class="eyebrow">PLAN AHEAD</span><h2>Create a trip</h2>
    <p>Add your destination and choose the card you plan to use.</p>
    <form class="trip-form" id="tripForm">
      <label>DESTINATION COUNTRY<input name="destinationCountry" placeholder="e.g. Japan" required></label>
      <label>CITY<input name="destinationCity" placeholder="e.g. Tokyo"></label>
      <div class="row"><label>START DATE<input name="startDate" type="date" min="${new Date().toISOString().slice(0,10)}" value="${start}" required></label>
      <label>END DATE<input name="endDate" type="date" min="${new Date().toISOString().slice(0,10)}" value="${end}" required></label></div>
      <div class="row"><label>BUDGET<input name="budget" type="number" min="0.01" step="0.01" value="2500" required></label>
      <label>CURRENCY<input name="budgetCurrency" maxlength="3" value="USD" pattern="[A-Za-z]{3}" required></label></div>
      <label>PREFERRED CARD<select name="preferredCardId" required>${cardOptions()}</select><small>The destination exchange rate is stored against this card when the journey is opened.</small></label>
      <button type="submit">Create trip</button>
    </form>`);
  $("#tripForm").onsubmit=createTrip;
}
async function createTrip(event){
  event.preventDefault();
  const form=new FormData(event.currentTarget);
  const payload=Object.fromEntries(form.entries());payload.budget=Number(payload.budget);payload.budgetCurrency=payload.budgetCurrency.toUpperCase();
  if(payload.endDate<payload.startDate){toast("End date must be on or after the start date");return}
  const button=event.currentTarget.querySelector("button");button.disabled=true;button.textContent="Creating…";
  try{
    const trip=await api("/api/travel/trips",{method:"POST",body:JSON.stringify(payload)});
    await load();
    openSheet(`<div class="success-panel"><b>Trip created</b><p>${trip.destinationCity||trip.destinationCountry}, ${trip.destinationCountry} has been added to your journeys.</p></div>
      <div class="menu-actions"><button onclick="showJourney('${trip.id}')">Open journey</button><button class="light" onclick="runCheck('${trip.id}')">Run readiness check</button></div>`);
  }catch(e){toast(e.message);button.disabled=false;button.textContent="Create trip"}
}

async function load(){
  try{
    const {data:trips}=await api("/api/travel/trips");
    trips.sort((a,b)=>(a.status==="COMPLETED")-(b.status==="COMPLETED")||a.startDate.localeCompare(b.startDate));
    const focusTrip=trips.find(t=>t.status!=="COMPLETED"&&t.status!=="CANCELLED")||trips[0];
    const [dashboard,{data:alerts},{data:cards},{data:transactions},{data:cases}]=await Promise.all([
      api(`/api/travel/trips/${focusTrip.id}/dashboard`),
      api("/api/travel/alerts"),api("/api/travel/cards"),api("/api/travel/transactions"),api("/api/travel/cases")
    ]);
    state={trips,dashboard,alerts,cards,transactions,cases};
    const upcoming=trips.filter(t=>t.status!=="COMPLETED"&&t.status!=="CANCELLED");
    $("#trips").innerHTML=(upcoming.length?upcoming:trips).map(t=>`<article class="trip" role="button" tabindex="0" onclick="showJourney('${t.id}')">
      <span class="tag">${t.status}</span><span class="arrow">→</span>
      <h3>${t.destinationCity}, ${t.destinationCountry}</h3>
      <p>${t.startDate} — ${t.endDate} · ${currencyMoney(t.budget,t.budgetCurrency)}</p>
    </article>`).join("");
    $("#spent").textContent=currencyMoney(dashboard.spent,dashboard.currency);
    $("#remaining").textContent=currencyMoney(dashboard.remaining,dashboard.currency);
    $("#budget").textContent=currencyMoney(dashboard.budget,dashboard.currency);
    renderTravelToolkit();
    const failed=dashboard.recentTransactions.find(t=>t.status==="DECLINED"&&t.failureCode==="NETWORK_ERROR")||dashboard.recentTransactions.find(t=>t.status==="DECLINED");
    const featuredByHero=renderHomeAssistant(failed);
    $("#attentionSection").style.display=failed&&!featuredByHero?"block":"none";
    $("#paymentIssue").innerHTML=failed?`<div class="issue-card interrupted-home" onclick="paymentHelp('${failed.transactionId}')">
      <div class="issue-icon">!</div><div><small>PAYMENT INTERRUPTED</small><h3>Complete your ${failed.merchantName} payment</h3><p>${currencyMoney(failed.billingAmount,failed.billingCurrency)} · We found the best next step</p></div><span class="chevron">→</span></div>`:"";
    const activeCases=cases.filter(c=>c.status!=="RESOLVED");
    $("#caseTracking").innerHTML=activeCases.length?`<div class="section-head compact-head"><div><span class="eyebrow">BANK FOLLOW-UP</span><h2>Cases in progress</h2></div><button onclick="showCases()">View all</button></div>${activeCases.slice(0,1).map(caseCard).join("")}`:"";
    maybeShowFraudAlert();
  }catch(e){toast(e.message)}
}
const caseCard=c=>`<div class="case-card" onclick="showCase('${c.id}')" role="button"><div class="case-top"><span>${c.status.replaceAll("_"," ")}</span><b>${c.id}</b></div><h3>${c.title}</h3><p>${c.currentUpdate}</p><small>Updated ${new Date(c.updatedAt).toLocaleString()}</small><div class="case-progress"><i></i><i class="${c.status!=="SUBMITTED"?"done":""}"></i><i class="${c.status==="RESOLVED"?"done":""}"></i></div></div>`;
window.showCases=()=>openSheet(`<button class="back" onclick="activate('home')">← Overview</button><span class="eyebrow">BANK CASE TRACKING</span><h2>Your support cases</h2><p>Follow what the bank is doing without calling for an update.</p>${state.cases.length?state.cases.map(caseCard).join(""):"<p>No support cases.</p>"}`);
window.showCase=id=>{
  const c=state.cases.find(x=>x.id===id);if(!c)return;
  openSheet(`<button class="back" onclick="showCases()">← All cases</button><span class="eyebrow">${c.type.replaceAll("_"," ")}</span><h2>${c.title}</h2><div class="case-reference"><small>CASE REFERENCE</small><b>${c.id}</b><span>${c.status.replaceAll("_"," ")}</span></div><div class="case-timeline"><div class="done"><b>Request received</b><small>${new Date(c.createdAt).toLocaleString()}</small></div><div class="${c.status!=="SUBMITTED"?"done":""}"><b>Bank review</b><small>${c.currentUpdate}</small></div><div class="${c.status==="RESOLVED"?"done":""}"><b>Resolution</b><small>${c.status==="RESOLVED"?"Completed":"We’ll update this step when the review is complete."}</small></div></div><button class="bank-secondary" onclick="activate('home')">Back to overview</button>`);
};
function renderHomeAssistant(failed){
  const next=state.trips.find(t=>t.status!=="COMPLETED"&&t.status!=="CANCELLED")||state.trips[0];
  if(!next)return false;
  const days=dayCount(next.startDate),when=days>1?`in ${days} days`:days===1?"tomorrow":days===0?"today":"in progress";
  $("#nextTripLabel").textContent=`${next.destinationCity||next.destinationCountry} ${when} · ${next.startDate}`;
  $("#openNextTrip").onclick=()=>showJourney(next.id);
  const openAlert=state.alerts.find(a=>a.status==="OPEN");
  const preferred=state.cards.find(c=>c.id===next.preferredCardId);
  let title="Your trip plan is ready to review",detail="See card, currency and budget advice prepared for this journey.",button="Open travel plan",action=()=>showJourney(next.id),score=state.dashboard.readinessStatus==="READY"?"✓":"!";
  if(openAlert){title="Confirm an unusual travel payment";detail=`Check ${openAlert.merchantName}, ${currencyMoney(openAlert.amount,openAlert.currency)} before we take action.`;button="Review payment";action=showAlerts;score="!"}
  else if(failed){title="Resolve your declined payment";detail=`We found the likely reason ${failed.merchantName} was declined and prepared the next step.`;button="Fix payment";action=()=>paymentHelp(failed.transactionId);score="!"}
  else if(preferred&&!preferred.overseasPaymentsEnabled){title="Turn on overseas payments";detail=`Your preferred ${preferred.cardType} ${preferred.maskedCardNumber} is not yet enabled abroad.`;button="Enable securely";action=()=>cardAction(preferred.id,"enable-overseas-payments");score="!"}
  else if(state.dashboard.readinessStatus!=="READY"){title="Complete your pre-travel check";detail="We’ll check card expiry, limits, balance, currency support and backup payment options.";button="Check trip readiness";action=()=>runCheck(next.id)}
  $("#assistantAction").textContent=title;$("#readinessText").textContent=detail;$("#assistantButton").textContent=button;$("#score").textContent=score;$("#checkBtn").onclick=action;
  return Boolean(openAlert||failed);
}
function renderTravelToolkit(){
  const next=state.trips.find(t=>t.status!=="COMPLETED"&&t.status!=="CANCELLED")||state.trips[0];
  const preferred=next&&state.cards.find(c=>c.id===next.preferredCardId);
  const location=next?(next.destinationCity||next.destinationCountry):"your next trip";
  $("#travelToolkitContext").textContent=`Quick access for ${location}—without repeating items already shown in your next best action.`;
  const cardTitle=preferred?(preferred.overseasPaymentsEnabled?"Preferred card is ready":"Finish setting up your preferred card"):"Choose a travel card";
  const cardDetail=preferred?`${preferred.cardType} ${preferred.maskedCardNumber} · Overseas payments ${preferred.overseasPaymentsEnabled?"on":"off"}`:"Select a card and check destination currency support";
  $("#travelActions").innerHTML=`
    <button onclick="${preferred&&!preferred.overseasPaymentsEnabled?`enableTravelCard('${preferred.id}')`:"activate('profile')"}">
      <b class="toolkit-icon">▣</b><span>${cardTitle}<small>${cardDetail}</small></span><i>→</i>
    </button>
    <button onclick="activate('transactions')">
      <b class="toolkit-icon">↗</b><span>Payments &amp; cash<small>Track spending and recover interrupted payments</small></span><i>→</i>
    </button>`;
}
window.enableTravelCard=async id=>{
  try{await api(`/api/travel/cards/${id}/enable-overseas-payments`,{method:"POST"});await load();toast("Your preferred card is ready for overseas payments")}
  catch(e){toast(e.message)}
};

async function runCheck(id="trip-tokyo"){
  try{
    const r=await api(`/api/travel/trips/${id}/readiness-check`,{method:"POST"});
    if(id==="trip-tokyo"){
      $("#score").textContent=r.score+"/100";$("#ring span").textContent=r.score;
      $("#readinessText").textContent=r.status==="READY"?"Everything is ready. Have a great trip!":"A few settings need attention before you leave.";
    }
    openSheet(`<button class="back" onclick="showJourney('${id}')">← Journey details</button>
      <h2>Readiness · ${r.score}/100</h2>
      ${r.checks.map(c=>`<div class="check ${c.passed?"":"fail"}"><b>${c.passed?"✓":"!"} ${c.message}</b>${c.recommendedAction?`<small>${c.recommendedAction}</small>`:""}${c.passed?"":readinessAction(id,c.ruleCode)}</div>`).join("")}`);
  }catch(e){toast(e.message)}
}
function readinessAction(tripId,ruleCode){
  const labels={OVERSEAS_PAYMENT_DISABLED:"Enable overseas payments",ONLINE_PAYMENT_DISABLED:"Enable online payments",
    CARD_FROZEN:"Unfreeze card",PAYMENT_LIMIT_LOW:"Increase payment limit",WITHDRAWAL_LIMIT_LOW:"Increase ATM limit",
    LOW_AVAILABLE_BALANCE:"View funding options",NO_BACKUP_CARD:"Choose a backup card",CARD_EXPIRED:"Choose another card",
    ACCOUNT_NOT_ACTIVE:"Review account"};
  return `<button class="readiness-action" onclick="fixReadiness('${tripId}','${ruleCode}')">${labels[ruleCode]||"View solution"} →</button>`;
}
window.fixReadiness=async(tripId,ruleCode)=>{
  const trip=state.trips.find(t=>t.id===tripId),cardId=trip.preferredCardId;
  try{
    if(ruleCode==="OVERSEAS_PAYMENT_DISABLED")await api(`/api/travel/cards/${cardId}/enable-overseas-payments`,{method:"POST"});
    else if(ruleCode==="ONLINE_PAYMENT_DISABLED")await api(`/api/travel/cards/${cardId}/enable-online-payments`,{method:"POST"});
    else if(ruleCode==="CARD_FROZEN"){secureCardRequest("CARD_UNFREEZE",cardId,`/api/travel/cards/${cardId}/unfreeze`,{method:"POST"},()=>runCheck(tripId));return}
    else if(ruleCode==="PAYMENT_LIMIT_LOW"){secureCardRequest("PAYMENT_LIMIT_CHANGE",cardId,`/api/travel/cards/${cardId}/payment-limit`,{method:"PUT",body:JSON.stringify({newLimit:2500})},()=>runCheck(tripId));return}
    else if(ruleCode==="WITHDRAWAL_LIMIT_LOW"){secureCardRequest("WITHDRAWAL_LIMIT_CHANGE",cardId,`/api/travel/cards/${cardId}/withdrawal-limit`,{method:"PUT",body:JSON.stringify({newLimit:1000})},()=>runCheck(tripId));return}
    else if(["CARD_EXPIRED","LOW_AVAILABLE_BALANCE","ACCOUNT_NOT_ACTIVE","NO_BACKUP_CARD"].includes(ruleCode)){showCardSolution(tripId,ruleCode);return}
    else if(["CURRENCY_UNKNOWN","CURRENCY_NOT_SUPPORTED"].includes(ruleCode)){showCurrencyGuidance(tripId,ruleCode);return}
    else{showCardSolution(tripId,ruleCode);return}
    await load();toast("Card setting updated");runCheck(tripId);
  }catch(e){toast(e.message)}
};
function showCardSolution(tripId,ruleCode){
  const trip=state.trips.find(t=>t.id===tripId),available=state.cards.filter(c=>c.status==="ACTIVE"&&c.id!==trip.preferredCardId);
  openSheet(`<button class="back" onclick="runCheck('${tripId}')">← Readiness check</button><span class="eyebrow">RECOMMENDED SOLUTION</span>
    <h2>Choose a better travel card</h2><p>${ruleCode==="LOW_AVAILABLE_BALANCE"?"Choose a card linked to an account with enough available funds.":"Select an active backup card for this journey."}</p>
    ${available.map(c=>`<div class="menu-card"><h3>${c.cardType} ${c.maskedCardNumber}</h3><p>Main currency ${c.mainCurrency||"—"} · Supports ${(c.supportedCurrencies||[]).join(", ")||"none listed"} · Limit ${currencyMoney(c.dailyPaymentLimit,c.mainCurrency)}</p><button class="inline-link" onclick="selectJourneyCard('${tripId}','${c.id}')">Use for this trip →</button></div>`).join("")||"<p>No other active card is available. Contact secure banking support to arrange a replacement.</p>"}`);
}
window.selectJourneyCard=async(tripId,cardId)=>{
  const t=state.trips.find(x=>x.id===tripId);
  try{await api(`/api/travel/trips/${tripId}`,{method:"PUT",body:JSON.stringify({destinationCountry:t.destinationCountry,destinationCity:t.destinationCity,startDate:t.startDate,endDate:t.endDate,budget:t.budget,budgetCurrency:t.budgetCurrency,preferredCardId:cardId})});await load();toast("Preferred card updated");runCheck(tripId)}catch(e){toast(e.message)}
};
function showCurrencyGuidance(tripId,ruleCode="CURRENCY_UNKNOWN"){openSheet(`<button class="back" onclick="runCheck('${tripId}')">← Readiness check</button><span class="eyebrow">CURRENCY SUPPORT</span><h2>${ruleCode==="CURRENCY_NOT_SUPPORTED"?"This card does not support the destination currency":"Destination currency could not be verified"}</h2><p>Choose a compatible preferred card in Trip Details. If no suitable card is available, record that you will exchange cash after arrival.</p><div class="support-steps"><div class="support-step"><b>Switch preferred card</b><small>We will immediately refresh the rate and rerun readiness.</small></div><div class="support-step"><b>Plan cash exchange</b><small>This records a backup payment plan for your journey.</small></div></div><button class="bank-primary" onclick="showCardSolution('${tripId}','${ruleCode}')">Choose another card</button><button class="bank-secondary" onclick="planCashExchange('${tripId}')">I’ll exchange cash after arrival</button>`)};
window.planCashExchange=async tripId=>{
  try{await api(`/api/travel/trips/${tripId}/cash-exchange-plan`,{method:"POST"});await load();toast("Cash exchange plan saved");runCheck(tripId)}catch(e){toast(e.message)}
};

window.showJourney=async id=>{
  try{
    const trip=state.trips.find(t=>t.id===id),[d,fx]=await Promise.all([api(`/api/travel/trips/${id}/dashboard`),api(`/api/travel/trips/${id}/exchange-rate?_=${Date.now()}`,{headers:{"Cache-Control":"no-cache"}})]);
    const card=state.cards.find(c=>c.id===trip.preferredCardId),used=Number(d.budgetUsagePercentage||0);
    const budgetAdvice=used>100?"Spending is above the planned budget. Review recent purchases before using more funds.":used>75?"Most of the budget has been used. Keep a closer eye on the remaining days.":used>0?`${Math.round(used)}% of the budget has been used and ${currencyMoney(d.remaining,d.currency)} remains.`:"No spending yet. Your full travel budget is still available.";
    const cardAdvice=fx.cardSupportsCurrency?`${card?.cardType||"Your card"} ${fx.maskedCardNumber} supports ${fx.destinationCurrency} and overseas payments are ${card?.overseasPaymentsEnabled?"enabled":"not enabled"}.`:`${card?.cardType||"Your card"} ${fx.maskedCardNumber} is not verified for this destination currency.`;
    openSheet(`<button class="back" onclick="activate('trips')">← All journeys</button>
      <span class="eyebrow">${trip.status}</span><h2>${trip.destinationCity}, ${trip.destinationCountry}</h2>
      <p>${trip.startDate} — ${trip.endDate}</p>
      <section class="assistant-brief"><span class="eyebrow">YOUR BANK’S TRAVEL ADVICE</span><h3>${d.activeFraudAlerts?"Review a security alert before spending":"Here’s what to know before you go"}</h3>
        <div class="assistant-insight"><b>Preferred card</b><p>${cardAdvice}</p>${!fx.cardSupportsCurrency?`<button onclick="showCardSolution('${id}','CURRENCY_NOT_SUPPORTED')">Choose a better card →</button>`:card&&!card.overseasPaymentsEnabled?`<button onclick="cardAction('${card.id}','enable-overseas-payments')">Enable overseas payments →</button>`:""}</div>
        <div class="assistant-insight"><b>Budget outlook</b><p>${budgetAdvice}</p>${d.transactionCount?`<button onclick="showJourneyPayments('${id}')">Review spending →</button>`:""}</div>
        <div class="assistant-insight"><b>Pay like a local</b><p>Choose ${fx.destinationCurrency||"the local currency"} at the terminal. Avoid merchant currency conversion and keep a backup payment method.</p></div>
      </section>
      <div class="detail-grid">
        <div><small>BUDGET</small><strong>${currencyMoney(d.budget,d.currency)}</strong></div>
        <div><small>REMAINING</small><strong>${currencyMoney(d.remaining,d.currency)}</strong></div>
        <div><small>SPENT</small><strong>${currencyMoney(d.spent,d.currency)}</strong></div>
        <div><small>TRANSACTIONS</small><strong>${d.transactionCount}</strong></div>
      </div>
      <button class="budget-edit" onclick="openBudgetEditor('${id}')">Adjust travel budget <span>→</span></button>
      <div class="journey-fx ${fx.cardSupportsCurrency?"":"fx-warning"}"><div class="fx-top"><div><small>PREFERRED CARD · ${fx.maskedCardNumber}</small><strong>${fx.destinationCurrency&&fx.rate?`1 ${fx.cardCurrency} = ${Number(fx.rate).toFixed(4)} ${fx.destinationCurrency}`:"Rate unavailable"}</strong></div><button onclick="showJourney('${id}')">↻</button></div>
      <p>${fx.recommendation}${fx.rateDate?` · ${fx.estimated?"Reference fallback":`Live indicative rate from ${fx.provider}`} · ${fx.rateDate}`:""}</p>
      ${!fx.destinationCurrencyVerified||!fx.cardSupportsCurrency?`<div class="menu-actions"><button onclick="showCardSolution('${id}','${fx.destinationCurrencyVerified?"CURRENCY_NOT_SUPPORTED":"CURRENCY_UNKNOWN"}')">Change card</button><button class="light" onclick="planCashExchange('${id}')">Plan cash exchange</button></div>`:""}</div>
      <div class="menu-actions"><button onclick="runCheck('${id}')">Run readiness check</button><button class="light" onclick="showJourneyPayments('${id}')">View payments</button><button class="light" onclick="showCardSolution('${id}','CHANGE_CARD')">Change preferred card</button></div>
      <h3>Recent activity</h3>${d.recentTransactions.length?d.recentTransactions.slice(0,3).map(transactionRow).join(""):"<p>No transactions yet.</p>"}`);
  }catch(e){toast(e.message)}
};
window.openBudgetEditor=id=>{
  const t=state.trips.find(x=>x.id===id);
  openSheet(`<button class="back" onclick="showJourney('${id}')">← Journey details</button><span class="eyebrow">TRAVEL BUDGET</span><h2>Adjust your budget</h2><p>Update the amount you plan to spend. Readiness and spending insights will use the new budget immediately.</p><form class="trip-form" id="budgetForm"><label>PLANNED BUDGET<input name="budget" type="number" min="0.01" step="0.01" value="${t.budget}" required></label><label>BUDGET CURRENCY<input value="${t.budgetCurrency}" disabled></label><button type="submit">Save budget</button></form>`);
  $("#budgetForm").onsubmit=event=>saveBudget(event,id);
};
async function saveBudget(event,id){
  event.preventDefault();const t=state.trips.find(x=>x.id===id),budget=Number(new FormData(event.currentTarget).get("budget"));
  const button=event.currentTarget.querySelector("button");button.disabled=true;button.textContent="Saving…";
  try{await api(`/api/travel/trips/${id}`,{method:"PUT",body:JSON.stringify({destinationCountry:t.destinationCountry,destinationCity:t.destinationCity,startDate:t.startDate,endDate:t.endDate,budget,budgetCurrency:t.budgetCurrency,preferredCardId:t.preferredCardId})});await load();toast("Travel budget updated");showJourney(id)}catch(e){toast(e.message);button.disabled=false;button.textContent="Save budget"}
}
function transactionRow(t){
  const recovered=(t.recoveryStatus||"").startsWith("COMPLETED_"),interrupted=t.status==="DECLINED";
  const label=recovered?"✓ Paid after retry":interrupted?"● Payment interrupted · Action needed":t.status;
  return `<div class="check ${interrupted?"interrupted":recovered?"recovered":""}"><b>${t.merchantName} · ${currencyMoney(t.billingAmount,t.billingCurrency)}</b>
    <small>${t.transactionTime.slice(0,10)} · ${label}</small>
    ${interrupted||recovered?`<button class="inline-link" onclick="paymentHelp('${t.transactionId}')">${recovered?"View recovery timeline":"Complete this payment"} →</button>`:""}</div>`;
}
window.showJourneyPayments=async id=>{
  try{const d=await api(`/api/travel/trips/${id}/dashboard`);openSheet(`<button class="back" onclick="showJourney('${id}')">← Journey</button><h2>Journey payments</h2>${d.recentTransactions.length?d.recentTransactions.map(transactionRow).join(""):"<p>No transactions yet.</p>"}`)}
  catch(e){toast(e.message)}
};
window.paymentHelp=async id=>{
  try{
    const r=await api(`/api/travel/transactions/${id}/recovery`);
    activeRecovery=r;
    const complete=r.status.startsWith("COMPLETED_");
    const primary=complete?`<button class="bank-primary" onclick="activate('transactions')">Done</button>`:
      r.recommendedAction==="RETRY_PAYMENT"?`<button class="bank-primary recovery-primary" onclick="confirmRecoveryRetry('${id}')">Retry payment safely</button>`:
      r.recommendedAction==="CHANGE_LIMIT"?`<button class="bank-primary" onclick="increaseLimit()">Review payment limit</button>`:
      r.recommendedAction==="ADD_FUNDS"?`<button class="bank-primary" onclick="fundingGuidance()">View funding options</button>`:
      r.recommendedAction==="REVIEW_SECURITY"?`<button class="bank-primary" onclick="showAlerts()">Review security alert</button>`:
      `<button class="bank-primary" onclick="secureSupport('${id}')">Contact secure support</button>`;
    openSheet(`<button class="back" onclick="activate('transactions')">← Transactions</button><span class="eyebrow">PAYMENT RECOVERY ASSISTANT</span><h2>${complete?"Payment completed":"Let’s complete this payment"}</h2>
      <div class="recovery-payment ${complete?"complete":""}"><div><small>${complete?"PAID AFTER RETRY":"PAYMENT INTERRUPTED"}</small><h3>${currencyMoney(r.amount,r.currency)}</h3><p>${r.merchantName} · ${r.merchantCity}, ${r.merchantCountry}<br>${r.maskedCardNumber}</p></div><span>${complete?"✓":"!"}</span></div>
      <div class="status-explainer"><b>${complete?"Only one payment was made":"Your payment didn’t go through yet"}</b><p>${r.safetyMessage}</p></div>
      <section class="ai-decision"><div class="ai-title"><span>✦</span><div><small>AI PAYMENT ANALYSIS · ${r.confidence}% CONFIDENCE</small><h3>${r.explanation}</h3></div></div><p>${r.travelContext}</p>
        <div class="recovery-checks">${r.checks.map(c=>`<div class="${c.passed?"pass":"warn"}"><i>${c.passed?"✓":"!"}</i><span><b>${c.label}</b><small>${c.detail}</small></span></div>`).join("")}</div>
      </section>
      ${complete?recoveryTimeline(r.timeline):`<div class="best-next"><small>BEST NEXT STEP</small><h3>${r.recommendedAction==="RETRY_PAYMENT"?"Retry this payment now":"Complete the required action"}</h3><p>${r.recommendedAction==="RETRY_PAYMENT"?"We’ll send a new authorization request. The previous interrupted attempt cannot be charged twice.":"Resolve the issue below, then return to complete the payment."}</p></div>`}
      ${primary}${!complete?`<div class="other-recovery">${r.checks.find(c=>c.label==="No fraud issue detected")?.passed?`<button onclick="useAnotherCard('${id}')">Use another card</button>`:`<button onclick="showAlerts()">Review security</button>`}<button onclick="contactMerchant('${id}')">Contact merchant</button><button onclick="secureSupport('${id}')">Contact bank</button></div><button class="ask-ai" onclick="askPaymentAssistant('${id}')">✦ Ask why this happened</button>`:""}`);
  }catch(e){toast(e.message)}
};
const recoveryTimeline=events=>`<div class="recovery-timeline"><h3>Payment timeline</h3>${events.map(e=>`<div><i>✓</i><span><b>${e.title}</b><small>${new Date(e.at).toLocaleString()} · ${e.detail}</small></span></div>`).join("")}</div>`;
window.confirmRecoveryRetry=async id=>{const r=await api(`/api/travel/transactions/${id}/recovery`);openSheet(`<button class="back" onclick="paymentHelp('${id}')">← Payment analysis</button><span class="eyebrow">SAFE RETRY</span><h2>Retry the payment?</h2><div class="retry-shield">↻</div><h3>We’ll reuse the original payment record</h3><p>A new authorization request will be sent to the merchant. The interrupted attempt cannot be collected or charged twice.</p><div class="safety-list"><span>✓ Card is working normally</span><span>✓ No fraud issue detected</span><span>✓ Duplicate charge protection</span></div><button class="bank-primary" onclick="performRecoveryRetry('${id}')">Retry ${currencyMoney(r.amount,r.currency)} payment</button><button class="bank-secondary" onclick="paymentHelp('${id}')">Not now</button>`)};
window.performRecoveryRetry=async id=>{
  openSheet(`<div class="recovery-processing"><div class="processing-ring">↻</div><span class="eyebrow">RECOVERY IN PROGRESS</span><h2>Completing your payment…</h2><p>Sending a new authorization securely. Keep this screen open.</p></div>`);
  try{await api(`/api/travel/transactions/${id}/recovery/retry`,{method:"POST"});await load();setTimeout(()=>paymentHelp(id),650)}catch(e){openSheet(`<span class="eyebrow">ACTION REQUIRED</span><h2>We couldn’t complete this payment</h2><p>The network issue is continuing, but your card is working normally and no money was taken.</p><button class="bank-primary" onclick="useAnotherCard('${id}')">Try another payment method</button><button class="bank-secondary" onclick="contactMerchant('${id}')">Contact merchant</button><button class="bank-secondary" onclick="secureSupport('${id}')">Contact bank support</button>`)}
};
window.contactMerchant=id=>openSheet(`<button class="back" onclick="paymentHelp('${id}')">← Payment recovery</button><span class="eyebrow">MERCHANT HELP</span><h2>Ask the merchant to restart their terminal</h2><p>Tell the merchant the bank confirmed your card is working and their payment network did not respond. Ask them to reconnect the terminal, then retry once.</p><div class="success-panel"><b>No money was taken</b><p>The interrupted authorization cannot be collected later.</p></div><button class="bank-primary" onclick="paymentHelp('${id}')">Return to payment</button>`);
window.askPaymentAssistant=async id=>{const r=await api(`/api/travel/transactions/${id}/recovery`);openSheet(`<button class="back" onclick="paymentHelp('${id}')">← Payment recovery</button><span class="eyebrow">AI PAYMENT ASSISTANT</span><h2>Ask about this payment</h2><div class="chat user">Why couldn’t I pay?</div><div class="chat assistant">${r.explanation} ${r.safetyMessage}</div><div class="chat user">Does it match my trip?</div><div class="chat assistant">${r.travelContext}</div><button class="bank-primary" onclick="paymentHelp('${id}')">Continue recovery</button>`)};
window.increaseLimit=async()=>{
  secureCardRequest("PAYMENT_LIMIT_CHANGE","card-002","/api/travel/cards/card-002/payment-limit",
    {method:"PUT",body:JSON.stringify({newLimit:2500})},()=>openSheet(`<div class="success-panel"><b>Payment limit updated</b><p>Your Mastercard daily payment limit is now $2,500. You can safely try the payment again.</p></div><button class="bank-primary" onclick="activate('payments')">Back to payments</button>`));
};
window.increaseAtmLimit=async()=>{
  secureCardRequest("WITHDRAWAL_LIMIT_CHANGE","card-002","/api/travel/cards/card-002/withdrawal-limit",
    {method:"PUT",body:JSON.stringify({newLimit:1000})},()=>openSheet(`<div class="success-panel"><b>ATM limit updated</b><p>Your daily withdrawal limit is now $1,000. Use a bank-owned ATM where possible and retry with the required amount.</p></div><button class="bank-primary" onclick="activate('transactions')">Back to transactions</button>`));
};
window.fundingGuidance=()=>openSheet(`<button class="back" onclick="activate('transactions')">← Transactions</button><span class="eyebrow">AVAILABLE FUNDS</span><h2>Choose a safe funding option</h2><div class="support-steps"><div class="support-step"><b>Transfer between your accounts</b><small>Open the main banking app and move available funds to the card’s linked account.</small></div><div class="support-step"><b>Use another active card</b><small>Avoid repeated retries while the balance is insufficient.</small></div></div><button class="bank-primary" onclick="useAnotherCard()">Choose another card</button><button class="bank-secondary" onclick="activate('transactions')">I’ll transfer in mobile banking</button>`);
window.secureSupport=async id=>{
  try{const c=await api(`/api/travel/transactions/${id}/support-case`,{method:"POST"});await load();openSheet(`<button class="back" onclick="paymentHelp('${id}')">← Payment help</button><span class="eyebrow">SECURE BANK SUPPORT</span><h2>Your case is being tracked</h2><div class="success-panel"><b>${c.id}</b><p>${c.currentUpdate}</p></div><button class="bank-primary" onclick="showCase('${c.id}')">Track this case</button><button class="bank-secondary" onclick="activate('transactions')">Back to transactions</button>`)}catch(e){toast(e.message)}
};
window.useAnotherCard=async(transactionId=activeRecovery?.transactionId)=>{
  if(!transactionId){toast("Open a payment recovery first");return}
  const r=activeRecovery?.transactionId===transactionId?activeRecovery:await api(`/api/travel/transactions/${transactionId}/recovery`);
  activeRecovery=r;
  openSheet(`<button class="back" onclick="paymentHelp('${transactionId}')">← Payment recovery</button><span class="eyebrow">ELIGIBLE BACKUP CARDS</span><h2>Complete with another card</h2><p>We checked card status, available funds, overseas payments and destination currency support.</p>${r.eligibleCards.length?r.eligibleCards.map(c=>`<div class="recovery-card"><div><small>READY FOR ${(r.merchantCountry||"DESTINATION").toUpperCase()}</small><h3>${c.cardType} ${c.maskedCardNumber}</h3><p>Main ${c.mainCurrency} · Supports ${c.supportedCurrencies.join(", ")}</p><span>✓ ${c.recommendation}</span></div><button onclick="confirmAlternateCard('${transactionId}','${c.cardId}')">Use card</button></div>`).join(""):`<div class="support"><h3>No eligible backup card</h3><p>Your other cards are frozen, have insufficient funds, have overseas payments disabled or do not support this destination currency.</p></div><button class="bank-primary" onclick="secureSupport('${transactionId}')">Contact bank support</button>`}`);
};
window.confirmAlternateCard=(transactionId,cardId)=>{
  const card=activeRecovery.eligibleCards.find(c=>c.cardId===cardId);
  openSheet(`<button class="back" onclick="useAnotherCard('${transactionId}')">← Choose card</button><span class="eyebrow">CONFIRM ALTERNATE CARD</span><h2>Pay with ${card.cardType} ${card.maskedCardNumber}?</h2><p>We’ll send one new authorization using this card and keep it under the original payment record.</p><div class="safety-list"><span>✓ Previous attempt cannot be charged</span><span>✓ Card and funds checked</span><span>✓ Destination currency supported</span></div><button class="bank-primary" onclick="completeWithAlternateCard('${transactionId}','${cardId}')">Complete ${currencyMoney(activeRecovery.amount,activeRecovery.currency)} payment</button><button class="bank-secondary" onclick="useAnotherCard('${transactionId}')">Choose a different card</button>`);
};
window.completeWithAlternateCard=async(transactionId,cardId)=>{
  openSheet(`<div class="recovery-processing"><div class="processing-ring">↻</div><span class="eyebrow">RECOVERY IN PROGRESS</span><h2>Completing with your backup card…</h2><p>Sending one secure authorization to the merchant.</p></div>`);
  try{await api(`/api/travel/transactions/${transactionId}/recovery/use-card`,{method:"POST",body:JSON.stringify({cardId})});await load();setTimeout(()=>paymentHelp(transactionId),650)}catch(e){toast(e.message);setTimeout(()=>useAnotherCard(transactionId),500)}
};

function activate(page){
  if(page==="payments")page="transactions";if(page==="security")page="profile";
  document.querySelectorAll("nav button").forEach(b=>b.classList.toggle("active",b.dataset.page===page));
  if(page==="home"){$("#contentPage").classList.add("hidden");$("#homePage").classList.remove("hidden");$("#sheet").classList.add("hidden");$("#homePage").scrollTo({top:0,behavior:"smooth"});return}
  if(page==="trips")openSheet(`<span class="eyebrow">YOUR JOURNEYS</span><h2>All journeys</h2><p>Select a journey to view readiness, budget and payments.</p><div class="menu-actions"><button onclick="openCreateTrip()">+ Create a trip</button></div>${state.trips.map(journeyCard).join("")}`);
  if(page==="transactions")renderTransactionsPage();
  if(page==="profile")renderProfilePage();
}
function renderTransactionsPage(){
  const approved=state.transactions.filter(t=>t.status==="APPROVED"&&t.transactionType!=="REFUND"),declined=state.transactions.filter(t=>t.status==="DECLINED");
  const spending={};approved.forEach(t=>spending[t.merchantCategory||"OTHER"]=(spending[t.merchantCategory||"OTHER"]||0)+Number(t.billingAmount));
  const entries=Object.entries(spending).sort((a,b)=>b[1]-a[1]),total=entries.reduce((s,e)=>s+e[1],0)||1;
  const colors=["#0d6b4b","#d5f16b","#e7a765","#6da7a0","#8b7cad","#d77a67","#9ba49e"];let cursor=0;
  const stops=entries.map((e,i)=>{const start=cursor;cursor+=e[1]/total*100;return `${colors[i%colors.length]} ${start}% ${cursor}%`}).join(",");
  const top=entries[0],observation=declined.length?`${declined.length} payment${declined.length>1?"s":""} need attention. Each has a bank-guided resolution path.`:top?`${top[0]} is your largest travel spending category at ${Math.round(top[1]/total*100)}%.`:"Your travel spending insights will appear after the first purchase.";
  openSheet(`<div class="page-title"><span class="eyebrow">YOUR TRAVEL MONEY</span><h1>Spending assistant</h1><p>Understand where your travel money goes and get help with declined payments.</p></div>
    <div class="spending-advice"><span class="eyebrow">BANK INSIGHT</span><b>${observation}</b></div>
    <div class="transaction-summary"><div><small>TOTAL SPENT</small><b>${money(total)}</b></div><div><small>APPROVED</small><b>${approved.length}</b></div><div><small>DECLINED</small><b>${declined.length}</b></div></div>
    <div class="chart-wrap"><div class="pie-chart" style="background:conic-gradient(${stops||"#dfe3dd 0 100%"})"></div><div class="chart-legend">${entries.slice(0,7).map((e,i)=>`<div><i style="background:${colors[i%colors.length]}"></i><span>${e[0]} · ${Math.round(e[1]/total*100)}%</span></div>`).join("")}</div></div>
    <h3>All travel transactions</h3>${state.transactions.map(transactionRow).join("")}`);
}
function physicalCard(c,index){
  const last4=(c.maskedCardNumber.match(/\d{4}$/)||["••••"])[0],expiry=`${String(c.expiryMonth).padStart(2,"0")}/${String(c.expiryYear).slice(-2)}`;
  const network=c.cardType==="MASTERCARD"?`<div class="mastercard-mark"><i></i><i></i></div>`:`<div class="visa-mark">VISA</div>`;
  return `<article class="card-wallet-item">
    <div class="physical-card card-theme-${index%5} ${c.status==="FROZEN"?"is-frozen":""}">
      <div class="card-brand"><b>VOYAGE</b><span>${c.status==="FROZEN"?"FROZEN":"WORLD CARD"}</span></div>
      <div class="card-chip"><i></i><i></i><i></i></div><div class="contactless">)))</div>
      <div class="embossed-number">•••• &nbsp;•••• &nbsp;•••• &nbsp;${last4}</div>
      <div class="card-bottom"><div><small>VALID THRU</small><strong>${expiry}</strong></div><div><small>MAIN CURRENCY</small><strong>${c.mainCurrency||"—"}</strong></div>${network}</div>
    </div>
    <div class="card-controls">
      <div class="card-capabilities"><span>${c.status}</span><span>Overseas ${c.overseasPaymentsEnabled?"on":"off"}</span><span>${(c.supportedCurrencies||[]).length} currencies</span></div>
      <p>Supports ${(c.supportedCurrencies||[]).join(", ")||"no additional currencies"}</p>
      <div class="menu-actions">${c.status==="FROZEN"?`<button onclick="cardAction('${c.id}','unfreeze')">Unfreeze card</button>`:`<button onclick="cardAction('${c.id}','freeze')">Freeze card</button>`}${!c.overseasPaymentsEnabled?`<button class="light" onclick="cardAction('${c.id}','enable-overseas-payments')">Enable overseas</button>`:""}</div>
    </div>
  </article>`;
}
function renderProfilePage(){
  openSheet(`<div class="page-title"><span class="eyebrow">YOUR BANKING PROFILE</span><h1>Profile</h1></div>
    <div class="profile-header"><div class="profile-avatar">JH</div><div><h3>Jessie Han</h3><p>Customer · 001 · Secure session active</p></div></div>
    <section class="section-head"><div><span class="eyebrow">TRAVEL CARDS</span><h2>Your cards</h2></div><small class="swipe-hint">Swipe →</small></section>
    <div class="card-wallet">${state.cards.map(physicalCard).join("")}</div>
    <section class="section-head"><div><span class="eyebrow">DEMO TOOLS</span><h2>Payment testing</h2></div></section>
    <div class="payment-test-launch">
      <div class="test-lab-icon">＋</div><div><h3>Record a test payment</h3><p>Run a payment through card, FX, travel and fraud checks.</p></div>
      <button onclick="openPaymentTest()">Open</button>
    </div>
    <section class="section-head"><div><span class="eyebrow">SECURITY</span><h2>Sign-in & alerts</h2></div></section>
    <div class="menu-card"><h3>Preferred verification</h3><p>Trusted device / Face ID · SMS remains available</p></div>
    <button class="bank-secondary" onclick="showAlerts()">Review fraud alerts</button><button class="bank-secondary" onclick="signOut()">Sign out securely</button>`);
}
window.openPaymentTest=()=>{
  const activeCards=state.cards.filter(c=>c.status==="ACTIVE");
  if(!activeCards.length){toast("No active card is available for testing");return}
  openSheet(`<button class="back" onclick="activate('profile')">← Profile</button>
    <span class="eyebrow">PAYMENT TEST LAB</span><h2>Record a test payment</h2>
    <p class="test-lab-intro">This demo uses the real transaction pipeline. The payment will be stored in MySQL and may create a recovery journey or fraud alert.</p>
    <form class="payment-test-form" id="paymentTestForm">
      <label>TEST SCENARIO<select name="scenario" onchange="applyPaymentTestScenario(this.value)">
        <option value="APPROVED">Successful travel payment</option>
        <option value="NETWORK_ERROR">Payment network timeout</option>
        <option value="INSUFFICIENT_FUNDS">Insufficient funds</option>
        <option value="LIMIT_EXCEEDED">Card limit exceeded</option>
        <option value="SUSPICIOUS">Suspicious high-value payment</option>
      </select><small>Choose a preset, then adjust the payment details if needed.</small></label>
      <label>CARD<select name="cardId" required>${activeCards.map(c=>`<option value="${c.id}">${c.cardType} ${c.maskedCardNumber} · ${c.mainCurrency}</option>`).join("")}</select></label>
      <label>MERCHANT<input name="merchantName" value="Tokyo Airport Taxi" maxlength="100" required></label>
      <div class="row"><label>AMOUNT<input name="amount" type="number" min="0.01" step="0.01" value="65.00" required></label>
      <label>CURRENCY<input name="currency" maxlength="3" pattern="[A-Za-z]{3}" value="JPY" required></label></div>
      <div class="row"><label>COUNTRY<input name="country" value="Japan" required></label>
      <label>CITY<input name="city" value="Tokyo"></label></div>
      <label>CATEGORY<select name="category">
        <option value="TRANSPORT">Transport</option><option value="HOTEL">Hotel</option>
        <option value="DINING">Dining</option><option value="RETAIL">Retail</option>
        <option value="LUXURY">Luxury</option><option value="ATM">ATM</option>
      </select></label>
      <label>PAYMENT TYPE<select name="transactionType">
        <option value="PURCHASE">Card purchase</option><option value="ONLINE_PURCHASE">Online purchase</option>
        <option value="ATM_WITHDRAWAL">ATM withdrawal</option>
      </select></label>
      <div class="test-pipeline"><span>Card capability</span><i>→</i><span>Live FX</span><i>→</i><span>Travel rules</span><i>→</i><span>Fraud decision</span></div>
      <button type="submit">Run payment test</button>
    </form>`);
  $("#paymentTestForm").onsubmit=submitPaymentTest;
};
window.applyPaymentTestScenario=scenario=>{
  const form=$("#paymentTestForm");if(!form)return;
  const presets={
    APPROVED:{merchantName:"Tokyo Airport Taxi",amount:"65.00",currency:"JPY",country:"Japan",city:"Tokyo",category:"TRANSPORT",transactionType:"PURCHASE"},
    NETWORK_ERROR:{merchantName:"Tokyo Airport Taxi",amount:"65.00",currency:"JPY",country:"Japan",city:"Tokyo",category:"TRANSPORT",transactionType:"PURCHASE"},
    INSUFFICIENT_FUNDS:{merchantName:"Shinjuku Hotel",amount:"780.00",currency:"JPY",country:"Japan",city:"Tokyo",category:"HOTEL",transactionType:"PURCHASE"},
    LIMIT_EXCEEDED:{merchantName:"Tokyo Department Store",amount:"2800.00",currency:"USD",country:"Japan",city:"Tokyo",category:"RETAIL",transactionType:"PURCHASE"},
    SUSPICIOUS:{merchantName:"Overseas Luxury Store",amount:"4200.00",currency:"USD",country:"Syria",city:"Damascus",category:"LUXURY",transactionType:"PURCHASE"}
  }[scenario];
  Object.entries(presets).forEach(([name,value])=>form.elements[name].value=value);
};
async function submitPaymentTest(event){
  event.preventDefault();
  const form=event.currentTarget,data=new FormData(form),scenario=data.get("scenario");
  const declined=scenario!=="APPROVED"&&scenario!=="SUSPICIOUS";
  const payload={
    transactionId:`test-${Date.now()}-${Math.random().toString(36).slice(2,7)}`,
    cardId:data.get("cardId"),merchantName:data.get("merchantName").trim(),
    merchantCountry:data.get("country").trim(),merchantCity:data.get("city").trim(),
    merchantCategory:data.get("category"),originalAmount:Number(data.get("amount")),
    originalCurrency:data.get("currency").toUpperCase(),transactionTime:new Date().toISOString(),
    transactionType:data.get("transactionType"),status:declined?"DECLINED":"APPROVED",
    failureCode:declined?scenario:null
  };
  const button=form.querySelector("button[type=submit]");button.disabled=true;button.textContent="Running bank checks…";
  try{
    const transaction=await api("/api/travel/transactions/events",{method:"POST",body:JSON.stringify(payload)});
    await load();
    const alert=state.alerts.find(a=>a.transactionId===transaction.transactionId&&a.status==="OPEN");
    const interrupted=transaction.status==="DECLINED";
    openSheet(`<button class="back" onclick="openPaymentTest()">← New test</button><span class="eyebrow">TEST RESULT</span>
      <div class="test-result ${alert?"risk":interrupted?"interrupted":"approved"}"><span>${alert?"!":interrupted?"↻":"✓"}</span>
        <small>${alert?"SECURITY REVIEW REQUIRED":interrupted?"PAYMENT INTERRUPTED":"PAYMENT APPROVED"}</small>
        <h2>${currencyMoney(transaction.originalAmount,transaction.originalCurrency)}</h2>
        <p>${transaction.merchantName} · ${transaction.merchantCountry}<br>${state.cards.find(c=>c.id===payload.cardId)?.maskedCardNumber||""}</p>
      </div>
      <div class="test-result-detail"><b>${alert?"Fraud monitoring created an alert":interrupted?"Payment Recovery is ready":"All checks completed"}</b>
        <p>${alert?"The internal rules, external score and customer profile identified this payment for customer confirmation.":interrupted?`The transaction was stored as interrupted with reason ${scenario.replaceAll("_"," ").toLowerCase()}. No money was taken.`:"The transaction was stored in MySQL and is now included in travel spending insights."}</p></div>
      ${alert?`<button class="bank-primary" onclick="showAlerts()">Review generated fraud alert</button>`:interrupted?`<button class="bank-primary" onclick="paymentHelp('${transaction.transactionId}')">Open Payment Recovery</button>`:`<button class="bank-primary" onclick="activate('transactions')">View in Transactions</button>`}
      <button class="bank-secondary" onclick="openPaymentTest()">Run another test</button>`);
  }catch(e){toast(e.message);button.disabled=false;button.textContent="Run payment test"}
}
window.runCheck=runCheck;
window.openCreateTrip=openCreateTrip;
window.cardAction=async(id,action)=>{
  if(action==="freeze"||action==="unfreeze"){
    const secureAction=action==="freeze"?"CARD_FREEZE":"CARD_UNFREEZE";
    secureCardRequest(secureAction,id,`/api/travel/cards/${id}/${action}`,{method:"POST"},()=>activate("security"));return;
  }
  try{await api(`/api/travel/cards/${id}/${action}`,{method:"POST"});toast("Card settings updated");await load();activate("security")}catch(e){toast(e.message)}
};
function showAlerts(){
  const open=state.alerts.filter(a=>a.status==="OPEN"),resolved=state.alerts.filter(a=>a.status!=="OPEN");
  openSheet(`<button class="back" onclick="activate('home')">← Overview</button><span class="eyebrow">CARD SECURITY</span><h2>${open.length?"Payments to confirm":"You’re all caught up"}</h2>
    <p class="security-intro">${open.length?`Check ${open.length} payment${open.length>1?"s":""}. We’ll only ask you to act on activity that still needs a decision.`:"There are no payments waiting for your confirmation."}</p>
    ${open.length?`<section class="alert-group"><span class="eyebrow">NEEDS YOUR RESPONSE</span>${open.map(alertCard).join("")}</section>`:`<div class="security-clear"><span>✓</span><b>No action needed</b><small>We’ll notify you if a payment needs checking.</small></div>`}
    ${resolved.length?`<section class="alert-group resolved-group"><div class="resolved-group-title"><span class="eyebrow">PAST REVIEWS</span><small>${resolved.length} record${resolved.length>1?"s":""}</small></div>${resolved.map(alertCard).join("")}</section>`:""}`);
}
function maybeShowFraudAlert(){
  if(!$("#contentPage").classList.contains("hidden")||!$("#authGate").classList.contains("hidden"))return;
  const alert=state.alerts.find(a=>a.status==="OPEN"),seen=alert&&localStorage.getItem("seen_fraud_alert");
  if(!alert||seen===alert.id)return;
  $("#fraudPopupBody").innerHTML=`<div class="fraud-badge">!</div><span class="eyebrow">SECURITY ALERT</span><h2>Is this your payment?</h2>
    <div class="fraud-payment"><strong>${currencyMoney(alert.amount,alert.currency)}</strong><b>${alert.merchantName}</b><small>${alert.maskedCardNumber} · ${alert.merchantCountry||"Location unavailable"}<br>${alert.transactionTime?new Date(alert.transactionTime).toLocaleString():"Time unavailable"}</small></div>
    <p>Check the amount, merchant, card, location and time before responding.</p>
    <div class="fraud-meta"><b>${alert.riskLevel} risk · ${alert.decision}</b><small>${alert.reasonCodes.join(" · ")}</small></div>
    <div class="menu-actions"><button onclick="reviewFraudPopup('${alert.id}')">Review & respond</button><button class="light" onclick="dismissFraudPopup('${alert.id}')">Not now</button></div>`;
  $("#fraudPopup").classList.remove("hidden");
}
window.dismissFraudPopup=id=>{localStorage.setItem("seen_fraud_alert",id);$("#fraudPopup").classList.add("hidden")};
window.reviewFraudPopup=id=>{dismissFraudPopup(id);showAlerts()};
window.resolveAlert=async(id,action)=>{
  if(action==="freeze-card"){const alert=state.alerts.find(a=>a.id===id);secureCardRequest("CARD_FREEZE",alert.cardId,`/api/travel/alerts/${id}/freeze-card`,{method:"POST"},showAlerts);return}
  try{await api(`/api/travel/alerts/${id}/${action}`,{method:"POST"});await load();toast(action==="report"?"Fraud reported and a case was opened":"Payment confirmed as yours");showAlerts()}catch(e){toast(e.message)}
};
document.querySelectorAll("nav button").forEach(b=>b.onclick=()=>activate(b.dataset.page));
$("#checkBtn").onclick=()=>runCheck();
const fraudDemo=$("#fraudDemo");
if(fraudDemo)fraudDemo.onclick=async()=>{
  const button=$("#fraudDemo");button.classList.add("loading");
  try{
    const id=`txn-demo-${Date.now()}`;
    await api("/api/travel/transactions/events",{method:"POST",body:JSON.stringify({
      transactionId:id,customerId:"customer-001",cardId:"card-002",merchantName:"Paris Luxury Boutique",
      merchantCountry:"France",merchantCity:"Paris",merchantCategory:"LUXURY",originalAmount:1800,
      originalCurrency:"USD",transactionTime:"2026-08-16T14:30:00Z",transactionType:"PURCHASE",status:"APPROVED"
    })});
    await load();const alert=state.alerts.find(a=>a.transactionId===id);
    openSheet(`<span class="eyebrow">FRAUD ENGINE DEMO</span><h2>Suspicious payment detected</h2>
      <div class="support"><h3>${alert?.riskLevel||"HIGH"} risk · ${alert?.decision||"REQUIRE_CONFIRMATION"}</h3>
      <p>France does not match the registered Japan trip, and $1,800 is unusually high for this customer.</p>
      <small>${alert?.reasonCodes.join(" · ")||"OUTSIDE_DESTINATION · HIGH_VALUE_TRANSACTION · CUSTOMER_PROFILE_ANOMALY"}</small></div>
      <div class="score-stack"><div><small>INTERNAL RULES</small><b>50%</b></div><div><small>EXTERNAL API</small><b>30%</b></div><div><small>BANK PROFILE</small><b>20%</b></div></div>
      <div class="menu-actions"><button onclick="showAlerts()">Review alert</button><button class="light" onclick="activate('home')">Close</button></div>`);
  }catch(e){toast(e.message)}finally{button.classList.remove("loading")}
};
const swagger=$("#swagger");if(swagger)swagger.onclick=()=>location.href="/swagger-ui.html";
$("#securityActivity").onclick=showAlerts;
$("#trackCases").onclick=showCases;
$("#close").onclick=()=>activate("home");
$("#refresh").onclick=async()=>{await load();toast("Journeys refreshed")};
$("#createTrip").onclick=openCreateTrip;

let authChallenge="";
let pendingStepUp=null;
function secureCardRequest(action,resourceId,path,options,onSuccess){
  pendingStepUp={action,resourceId,path,options,onSuccess};
  const label={CARD_FREEZE:"freeze this card",CARD_UNFREEZE:"unfreeze this card",PAYMENT_LIMIT_CHANGE:"change the payment limit",WITHDRAWAL_LIMIT_CHANGE:"change the ATM limit"}[action]||"complete this action";
  openSheet(`<div class="stepup-panel"><div class="stepup-shield">✓</div><span class="eyebrow">ADDITIONAL SECURITY</span><h2>Verify it’s you</h2>
    <p>For your protection, verify again before you ${label}.</p>
    <button class="auth-method preferred" onclick="verifyStepUp('TRUSTED_DEVICE','trusted-device-demo')"><b>Continue with Face ID</b><small>Recommended · no text message needed</small></button>
    <button class="auth-method" onclick="showStepUpPin()"><b>Use Banking App PIN</b><small>Works without mobile signal</small></button>
    <button class="auth-method" onclick="showStepUpSms()"><b>Use text message</b><small>Send a one-time code to ••• ••• 0188</small></button>
    <button class="bank-secondary" onclick="activate('home')">Cancel</button></div>`);
}
window.verifyStepUp=async(method,credential)=>{
  try{
    const grant=await api("/api/auth/step-up",{method:"POST",body:JSON.stringify({action:pendingStepUp.action,resourceId:pendingStepUp.resourceId,method,credential})});
    const headers={"X-Step-Up-Token":grant.stepUpToken};
    await api(pendingStepUp.path,{...pendingStepUp.options,headers:{...pendingStepUp.options.headers,...headers}});
    const success=pendingStepUp.onSuccess;pendingStepUp=null;await load();toast("Identity verified and action completed");success?.();
  }catch(e){toast(e.message)}
};
window.showStepUpPin=()=>openSheet(`<button class="back" onclick="secureCardRequest(pendingStepUp.action,pendingStepUp.resourceId,pendingStepUp.path,pendingStepUp.options,pendingStepUp.onSuccess)">← Verification options</button><span class="eyebrow">APP PIN</span><h2>Enter Banking App PIN</h2><p>Demo PIN: <b>2580</b></p><div class="auth-code"><input id="stepPin" maxlength="4" inputmode="numeric"></div><button class="bank-primary" onclick="verifyStepUp('APP_PIN',$('#stepPin').value)">Verify and continue</button>`);
window.showStepUpSms=()=>openSheet(`<button class="back" onclick="secureCardRequest(pendingStepUp.action,pendingStepUp.resourceId,pendingStepUp.path,pendingStepUp.options,pendingStepUp.onSuccess)">← Verification options</button><span class="eyebrow">TEXT MESSAGE</span><h2>Enter the security code</h2><p>Sent to ••• ••• 0188. Demo code: <b>246810</b></p><div class="auth-code"><input id="stepSms" maxlength="6" inputmode="numeric"></div><button class="bank-primary" onclick="verifyStepUp('SMS_OTP',$('#stepSms').value)">Verify and continue</button>`);
function showAuth(){authToken="";localStorage.removeItem("traveller_access_token");$("#authGate").classList.remove("hidden")}
function preferredAuth(c){
  authChallenge=c.challengeId;$("#authBody").innerHTML=`<span class="eyebrow">RECOMMENDED</span><h2>Verify on this device</h2>
    <p>Use Face ID or your device passkey. No text message is needed.</p>
    <button class="auth-method preferred" onclick="verifyTrustedDevice()"><b>Continue with Face ID</b><small>Fastest · phishing-resistant in production</small></button>
    <button class="auth-link" onclick="showAlternativeAuth('${c.maskedPhone}')">Use another verification method</button>`;
}
async function startAuth(){
  try{preferredAuth(await api("/api/auth/start",{method:"POST",body:JSON.stringify({customerId:$("#authCustomer").value})}))}
  catch(e){toast(e.message)}
}
window.verifyTrustedDevice=()=>verifyAuth("TRUSTED_DEVICE","trusted-device-demo");
window.verifyAuth=async(method,credential)=>{
  try{
    const result=await api("/api/auth/verify",{method:"POST",body:JSON.stringify({challengeId:authChallenge,method,credential})});
    authToken=result.accessToken;localStorage.setItem("traveller_access_token",authToken);$("#authGate").classList.add("hidden");toast("Identity verified securely");load();
  }catch(e){toast(e.message)}
};
window.showAlternativeAuth=masked=>{$("#authBody").innerHTML=`<button class="auth-link" onclick="restartAuth()">← Back</button><h2>Other ways to verify</h2>
  <button class="auth-method preferred" onclick="showPinEntry()"><b>Banking App PIN</b><small>Works without mobile signal</small></button>
  <button class="auth-method" onclick="sendSmsCode()"><b>Text message</b><small>Send a one-time code to ${masked}</small></button>`};
window.showPinEntry=()=>{$("#authBody").innerHTML=`<button class="auth-link" onclick="restartAuth()">← Back</button><h2>Enter App PIN</h2><p>For this demo, use <b>2580</b>.</p><div class="auth-code"><input id="pinCode" maxlength="4" inputmode="numeric" autocomplete="one-time-code"></div><button class="bank-primary" onclick="verifyAuth('APP_PIN',$('#pinCode').value)">Verify PIN</button>`};
window.sendSmsCode=async()=>{try{const sms=await api(`/api/auth/sms?challengeId=${authChallenge}`,{method:"POST"});$("#authBody").innerHTML=`<button class="auth-link" onclick="restartAuth()">← Back</button><h2>Enter text message code</h2><p>Code sent to ${sms.maskedPhone}. Demo code: <b>${sms.demoCode}</b></p><div class="auth-code"><input id="smsCode" maxlength="6" inputmode="numeric" autocomplete="one-time-code"></div><button class="bank-primary" onclick="verifyAuth('SMS_OTP',$('#smsCode').value)">Verify code</button>`}catch(e){toast(e.message)}};
window.restartAuth=()=>{$("#authBody").innerHTML=`<h2>Welcome back</h2><p>Verify your identity to open Travel Assistant.</p><label class="auth-label">CUSTOMER ID<input id="authCustomer" value="customer-001"></label><button class="bank-primary" id="authStart">Continue securely</button>`;$("#authStart").onclick=startAuth};
window.signOut=async()=>{try{if(authToken)await api("/api/auth/logout",{method:"POST"})}catch(e){}showAuth();restartAuth()};
$(".avatar").onclick=signOut;
$("#authStart").onclick=startAuth;
(async()=>{if(!authToken){showAuth();return}try{await api("/api/auth/session");$("#authGate").classList.add("hidden");load()}catch(e){showAuth()}})();
