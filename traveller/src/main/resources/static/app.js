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
let state={trips:[],dashboard:null,alerts:[],cards:[],transactions:[],cases:[],tripMoney:{}};
let activeRecovery=null;
let activeAtmMap=null;
let activeAnalyticsMap=null;
let currentAtms=[];
let activeAtmTripId="";
let atmSearchInFlight=false;
let activeRootPage="home";
let activeJourneyOrigin="home";
const tripRef={countries:[],citiesByCountry:new Map(),currenciesByCountry:new Map()};
let tripPickerOutsideHandlerBound=false;
const CURRENCY_PASS_CODES=new Set(["CURRENCY_SUPPORT","USD_SETTLEMENT_CARD","USD_SETTLEMENT_ACCEPTED","ATM_CASH_PLANNED","CASH_EXCHANGE_PLANNED"]);
const normalizeOverviewOrigin=origin=>origin==="trips"?"trips":"home";
const overviewBackLabel=origin=>normalizeOverviewOrigin(origin)==="trips"?"My trips":"Overview";
const overviewBackAction=origin=>`activate('${normalizeOverviewOrigin(origin)}')`;

const countryDisplay=c=>`${c.name} (${c.iso2}/${c.iso3})`;
const currencyDisplay=c=>`${c.code} - ${c.name}`;

function resolveCountrySelection(raw){
  const value=(raw||"").trim();
  if(!value)return null;
  return tripRef.countries.find(c=>
    c.name.toLowerCase()===value.toLowerCase()
    || countryDisplay(c).toLowerCase()===value.toLowerCase()
    || c.iso2.toLowerCase()===value.toLowerCase()
    || c.iso3.toLowerCase()===value.toLowerCase()
  )||null;
}

function parseCurrencyCode(raw){
  const value=(raw||"").trim().toUpperCase();
  if(!value)return "";
  const m=value.match(/^[A-Z]{3}/);
  return m?m[0]:value;
}

function isCountrySelected(option, currentValue){
  const value=(currentValue||"").trim().toLowerCase();
  return value && [option.name,countryDisplay(option),option.iso2,option.iso3].some(v=>(v||"").toLowerCase()===value);
}

function isCurrencySelected(option, currentValue){
  const value=(currentValue||"").trim().toLowerCase();
  if(!value)return false;
  const code=parseCurrencyCode(value);
  return [option.code,currencyDisplay(option)].some(v=>(v||"").toLowerCase()===value)||option.code.toLowerCase()===code.toLowerCase();
}

function closeTripPanels(){
  ["#countryPanel","#cityPanel","#currencyPanel"].forEach(id=>$(id)?.classList.remove("show"));
}

function openTripPanel(panelId){
  closeTripPanels();
  $(panelId)?.classList.add("show");
}

async function ensureTripReference(){
  if(tripRef.countries.length)return;
  const [{data:countries}]=await Promise.all([api("/api/public/reference/countries")]);
  tripRef.countries=countries||[];
}
function renderCountryOptions(filter=""){
  const list=$("#countryPanel");if(!list)return;
  const q=filter.trim().toLowerCase();
  const rows=tripRef.countries.filter(c=>!q||[c.name,c.iso2,c.iso3].some(v=>(v||"").toLowerCase().includes(q)));
  list.innerHTML=rows.length
    ? rows.map(c=>`<button type="button" class="trip-picker-option ${isCountrySelected(c,$("#countryInput")?.value)?"selected":""}" data-value="${escapeHtml(countryDisplay(c))}" aria-selected="${isCountrySelected(c,$("#countryInput")?.value)}">${escapeHtml(countryDisplay(c))}</button>`).join("")
    : '<div class="trip-picker-empty">No match</div>';
}
async function loadCities(country){
  if(!country)return [];
  if(tripRef.citiesByCountry.has(country))return tripRef.citiesByCountry.get(country);
  const {data}=await api(`/api/public/reference/cities?country=${encodeURIComponent(country)}`);
  tripRef.citiesByCountry.set(country,data||[]);
  return data||[];
}
async function loadCurrencies(country){
  const key=country||"__all__";
  if(tripRef.currenciesByCountry.has(key))return tripRef.currenciesByCountry.get(key);
  const {data}=await api(`/api/public/reference/currencies${country?`?country=${encodeURIComponent(country)}`:""}`);
  tripRef.currenciesByCountry.set(key,data||[]);
  return data||[];
}
function renderCityOptions(cities,filter=""){
  const list=$("#cityPanel");if(!list)return;
  const q=filter.trim().toLowerCase();
  const rows=(cities||[]).filter(x=>!q||(x.name||"").toLowerCase().includes(q));
  list.innerHTML=rows.length
    ? rows.map(x=>`<button type="button" class="trip-picker-option ${((x.name||"").trim().toLowerCase()===(($("#cityInput")?.value||"").trim().toLowerCase()))?"selected":""}" data-value="${escapeHtml(x.name)}" aria-selected="${((x.name||"").trim().toLowerCase()===(($("#cityInput")?.value||"").trim().toLowerCase()))}">${escapeHtml(x.name)}</button>`).join("")
    : '<div class="trip-picker-empty">No match</div>';
}
function renderCurrencyOptions(currencies,filter=""){
  const list=$("#currencyPanel");if(!list)return;
  const q=filter.trim().toLowerCase();
  const rows=(currencies||[]).filter(x=>!q||[x.code,x.name].some(v=>(v||"").toLowerCase().includes(q)));
  list.innerHTML=rows.length
    ? rows.map(x=>`<button type="button" class="trip-picker-option ${isCurrencySelected(x,$("#currencyInput")?.value)?"selected":""}" data-value="${escapeHtml(currencyDisplay(x))}" aria-selected="${isCurrencySelected(x,$("#currencyInput")?.value)}">${escapeHtml(currencyDisplay(x))}</button>`).join("")
    : '<div class="trip-picker-empty">No match</div>';
}
async function bindTripReferenceControls({countryValue="",cityValue="",currencyValue=""}={}){
  await ensureTripReference();
  const countryInput=$("#countryInput"),cityInput=$("#cityInput"),currencyInput=$("#currencyInput");
  const countryPanel=$("#countryPanel"),cityPanel=$("#cityPanel"),currencyPanel=$("#currencyPanel");

  countryInput.value=countryValue;
  cityInput.value=cityValue;
  renderCountryOptions();
  const initialCurrencies=await loadCurrencies("");
  renderCurrencyOptions(initialCurrencies,"");
  currencyInput.value=currencyValue|| (initialCurrencies.length?currencyDisplay(initialCurrencies[0]):"");

  const refreshByCountry=async(resetCity=true)=>{
    const selected=resolveCountrySelection(countryInput.value);
    const country=selected?.name||countryInput.value.trim();
    if(selected)countryInput.value=countryDisplay(selected);
    const [cities,currencies]=await Promise.all([loadCities(country),loadCurrencies(country)]);
    renderCityOptions(cities,cityInput?.value||"");
    renderCurrencyOptions(currencies,currencyInput?.value||"");
    if(resetCity)cityInput.value="";
    if(!currencyInput.value&&currencies.length)currencyInput.value=currencyDisplay(currencies[0]);
  };

  countryInput.onfocus=()=>{renderCountryOptions(countryInput.value);openTripPanel("#countryPanel")};
  countryInput.oninput=()=>{renderCountryOptions(countryInput.value);openTripPanel("#countryPanel")};
  countryInput.onchange=()=>refreshByCountry(true);

  cityInput.onfocus=()=>{
    const key=(resolveCountrySelection(countryInput.value)?.name||countryInput.value||"").trim();
    renderCityOptions(tripRef.citiesByCountry.get(key)||[],cityInput.value);
    openTripPanel("#cityPanel");
  };
  cityInput.oninput=()=>{
    const key=(resolveCountrySelection(countryInput.value)?.name||countryInput.value||"").trim();
    renderCityOptions(tripRef.citiesByCountry.get(key)||[],cityInput.value);
    openTripPanel("#cityPanel");
  };

  currencyInput.onfocus=()=>{
    const key=(resolveCountrySelection(countryInput.value)?.name||countryInput.value||"").trim();
    renderCurrencyOptions(tripRef.currenciesByCountry.get(key)||tripRef.currenciesByCountry.get("__all__")||[],currencyInput.value);
    openTripPanel("#currencyPanel");
  };
  currencyInput.oninput=()=>{
    const key=(resolveCountrySelection(countryInput.value)?.name||countryInput.value||"").trim();
    renderCurrencyOptions(tripRef.currenciesByCountry.get(key)||tripRef.currenciesByCountry.get("__all__")||[],currencyInput.value);
    openTripPanel("#currencyPanel");
  };

  countryPanel.onclick=async e=>{
    const option=e.target.closest(".trip-picker-option");if(!option)return;
    countryInput.value=option.dataset.value||"";
    closeTripPanels();
    await refreshByCountry(true);
  };
  cityPanel.onclick=e=>{
    const option=e.target.closest(".trip-picker-option");if(!option)return;
    cityInput.value=option.dataset.value||"";
    closeTripPanels();
  };
  currencyPanel.onclick=e=>{
    const option=e.target.closest(".trip-picker-option");if(!option)return;
    currencyInput.value=option.dataset.value||"";
    closeTripPanels();
  };

  if(!tripPickerOutsideHandlerBound){
    document.addEventListener("click",e=>{if(!e.target.closest(".trip-picker-wrap"))closeTripPanels()});
    tripPickerOutsideHandlerBound=true;
  }
}


async function api(path,opt={}){
  const headers={"Content-Type":"application/json",...opt.headers};
  if(authToken)headers.Authorization=`Bearer ${authToken}`;
  const r=await fetch(path,{...opt,headers});
  if(!r.ok){const e=await r.json().catch(()=>({message:"Request failed"}));if(r.status===401&&path.startsWith("/api/travel"))showAuth();throw Error(e.message)}
  if(r.status===204)return null;
  const raw=await r.text();
  if(!raw||!raw.trim())return null;
  try{return JSON.parse(raw);}catch{return null;}
}
function currencyCheckPassed(readiness){
  if(!readiness?.checks)return false;
  return readiness.checks.some(c=>c.passed&&CURRENCY_PASS_CODES.has(c.ruleCode));
}
function hasFailedReadiness(readiness){
  return Array.isArray(readiness?.checks)&&readiness.checks.some(c=>!c.passed);
}
async function runReadinessCheckSilently(tripId){
  try{return await api(`/api/travel/trips/${tripId}/readiness-check`,{method:"POST"});}
  catch{return null;}
}
function readinessBadge(tripId){
  const readiness=state.tripReadiness?.[tripId];
  if(!readiness)return `<button class="trip-readiness-badge pending" title="Readiness pending" aria-label="Readiness pending" onclick="showTripReadinessStatus('${tripId}',event)">·</button>`;
  const pass=readiness.status==="READY";
  return `<button class="trip-readiness-badge ${pass?"pass":"fail"}" title="${pass?"Readiness passed":"Readiness issue"}" aria-label="${pass?"Readiness passed":"Readiness issue"}" onclick="showTripReadinessStatus('${tripId}',event)">${pass?"✓":"!"}</button>`;
}
function pendingReadinessTrips(){
  const activeTrips=state.trips
    .filter(t=>t.status!=="COMPLETED"&&t.status!=="CANCELLED")
    .sort((a,b)=>a.startDate.localeCompare(b.startDate));
  return activeTrips.filter(t=>{
    const readiness=state.tripReadiness?.[t.id];
    if(!readiness)return true;
    if(hasFailedReadiness(readiness))return false;
    return readiness.status!=="READY";
  });
}
function dismissReadinessTodo(tripId,showToast=true){
  const card=document.querySelector(`#readinessTodoStack .readiness-todo-top[data-trip-id="${tripId}"]`);
  if(!card){load();return;}
  card.classList.add("exit");
  setTimeout(async()=>{
    await load();
    if(showToast)toast("Readiness passed. Switched to the next pending trip.");
  },360);
}
function tripsWithReadinessIssues(){
  const activeTrips=state.trips
    .filter(t=>t.status!=="COMPLETED"&&t.status!=="CANCELLED")
    .sort((a,b)=>a.startDate.localeCompare(b.startDate));
  const items=[];
  activeTrips.forEach(trip=>{
    const readiness=state.tripReadiness?.[trip.id];
    if(!readiness?.checks)return;
    const firstFailed=readiness.checks.find(c=>!c.passed);
    if(firstFailed)items.push({trip,check:firstFailed});
  });
  return items;
}
function readinessIssueCardHtml(trip,check,layer=""){
  const failedCount=state.tripReadiness?.[trip.id]?.checks?.filter(c=>!c.passed).length||1;
  const moreHint=failedCount>1?`<small class="readiness-issue-more">${failedCount-1} more to resolve after this</small>`:"";
  const isTop=layer==="readiness-issue-top"||!layer;
  return `<article class="readiness-issue-card ${layer}" data-trip-id="${trip.id}">
    <div class="readiness-issue-meta"><small>READINESS ISSUE</small><b>${trip.startDate}</b></div>
    <h3>${escapeHtml(trip.destinationCity||trip.destinationCountry)}, ${escapeHtml(trip.destinationCountry)}</h3>
    <p class="readiness-issue-alert">! ${escapeHtml(check.message)}</p>
    ${check.recommendedAction?`<p class="readiness-issue-desc">${escapeHtml(check.recommendedAction)}</p>`:""}
    ${moreHint}
    ${isTop?readinessAction(trip.id,check.ruleCode):""}
  </article>`;
}
function renderReadinessIssueStack(){
  const host=$("#readinessIssueStack");
  if(!host)return;
  const items=tripsWithReadinessIssues();
  if(!items.length){
    host.hidden=true;
    host.classList.remove("stacked","single");
    host.innerHTML="";
    return;
  }
  host.classList.toggle("single",items.length===1);
  host.classList.toggle("stacked",items.length>1);
  if(items.length===1){
    host.hidden=false;
    const {trip,check}=items[0];
    host.innerHTML=readinessIssueCardHtml(trip,check);
    return;
  }
  const [first,second,third]=items;
  const edge=()=>`<article class="readiness-issue-card readiness-issue-edge" aria-hidden="true"></article>`;
  host.hidden=false;
  host.innerHTML=`${readinessIssueCardHtml(first.trip,first.check,"readiness-issue-top")}${second?edge():""}${third?edge():""}`;
}
function dismissReadinessIssue(tripId){
  const card=document.querySelector(`#readinessIssueStack .readiness-issue-top[data-trip-id="${tripId}"]`)
    ||document.querySelector(`#readinessIssueStack .readiness-issue-card[data-trip-id="${tripId}"]`);
  if(!card){load();return;}
  card.classList.add("exit");
  setTimeout(()=>load(),360);
}
function updateReadinessAfterFix(tripId,readiness){
  if(readiness)state.tripReadiness={...(state.tripReadiness||{}),[tripId]:readiness};
  if(readiness?.status==="READY"){dismissReadinessTodo(tripId,false);return;}
  if(hasFailedReadiness(readiness))dismissReadinessIssue(tripId);
  else load();
}
function renderReadinessTodoStack(){
  const host=$("#readinessTodoStack");
  if(!host)return;
  const pending=pendingReadinessTrips();
  if(!pending.length){
    host.hidden=true;
    host.classList.remove("stacked","single");
    host.innerHTML="";
    return;
  }
  host.classList.toggle("single",pending.length===1);
  host.classList.toggle("stacked",pending.length>1);
  const [first,second,third]=pending;
  if(pending.length===1){
    host.hidden=false;
    host.innerHTML=`<article class="readiness-todo-card" data-trip-id="${first.id}">
      <div class="readiness-todo-meta"><small>READINESS TODO</small><b>${first.startDate}</b></div>
      <h3>${escapeHtml(first.destinationCity||first.destinationCountry)}, ${escapeHtml(first.destinationCountry)}</h3>
      <p>${state.tripReadiness?.[first.id]?"This trip still has readiness issues.":"Run readiness check for this new trip."}</p>
      <div class="menu-actions"><button onclick="showTripReadinessStatus('${first.id}')">View readiness checklist</button><button class="light" onclick="showJourney('${first.id}')">View trip</button></div>
    </article>`;
    return;
  }
  const one=(trip,layer)=>trip?`<article class="readiness-todo-card ${layer}" data-trip-id="${trip.id}">
      <div class="readiness-todo-meta"><small>READINESS TODO</small><b>${trip.startDate}</b></div>
      <h3>${escapeHtml(trip.destinationCity||trip.destinationCountry)}, ${escapeHtml(trip.destinationCountry)}</h3>
      <p>${state.tripReadiness?.[trip.id]?"This trip still has readiness issues.":"Run readiness check for this new trip."}</p>
      ${layer==="readiness-todo-top"?`<div class="menu-actions"><button onclick="showTripReadinessStatus('${trip.id}')">View readiness checklist</button><button class="light" onclick="showJourney('${trip.id}')">View trip</button></div>`:""}
    </article>`:"";
  const edge=trip=>trip?`<article class="readiness-todo-card readiness-todo-edge" aria-hidden="true"></article>`:"";
  host.hidden=false;
  host.innerHTML=`${one(first,"readiness-todo-top")}${edge(second)}${edge(third)}`;
}
window.showTripReadinessStatus=async(tripId,event,origin)=>{
  if(event){event.preventDefault();event.stopPropagation();}
  const source=normalizeOverviewOrigin(origin||activeRootPage||activeJourneyOrigin);
  const backLabel=overviewBackLabel(source);
  const backAction=overviewBackAction(source);
  let readiness=state.tripReadiness?.[tripId]||null;
  if(!readiness){
    try{readiness=await api(`/api/travel/trips/${tripId}/readiness`);}catch{readiness=null;}
  }
  if(!readiness){
    openSheet(`<button class="back" onclick="${backAction}">← ${backLabel}</button><span class="eyebrow">READINESS STATUS</span><h2>No readiness check yet</h2><p>Run the check to generate a pass/fail checklist for this trip.</p><button class="bank-primary" onclick="runCheck('${tripId}')">Run readiness check</button>`);
    return;
  }
  state.tripReadiness={...(state.tripReadiness||{}),[tripId]:readiness};
  openSheet(`<button class="back" onclick="${backAction}">← ${backLabel}</button><span class="eyebrow">READINESS STATUS</span><h2>${readiness.status==="READY"?"All checks passed":"Checks need attention"}</h2>
    ${readiness.checks.map(c=>`<div class="check ${c.passed?"":"fail"}"><b>${c.passed?"✓":"!"} ${c.message}</b>${c.recommendedAction?`<small>${c.recommendedAction}</small>`:""}${c.passed?"":readinessAction(tripId,c.ruleCode)}</div>`).join("")}
    <button class="bank-secondary" onclick="runCheck('${tripId}')">Run check again</button>`);
};
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
const uniqueByTransaction=items=>{
  const seen=new Set();
  return items.filter(item=>{const key=item.transactionId||item.id;if(seen.has(key))return false;seen.add(key);return true});
};
const alertPayment=a=>`<div class="alert-payment"><strong>${currencyMoney(a.amount,a.currency)}</strong><b>${a.merchantName}</b>
  <small>${a.maskedCardNumber} · ${a.merchantCountry||"Location unavailable"}<br>${a.transactionTime?new Date(a.transactionTime).toLocaleString():"Time unavailable"}</small></div>`;
const alertCard=a=>{
  const reasons=friendlyFraudReasons(a),status=a.status;
  if(status==="OPEN")return `<article class="fraud-alert-card alert-open">
    <div class="alert-status"><div><small>ACTION NEEDED</small><h3>Is this your payment?</h3></div><span>${a.riskLevel} RISK</span></div>
    ${alertPayment(a)}
    <p>Check the payment details before you respond.</p>
    <div class="reason-summary"><b>Why we’re checking</b>${reasons.slice(0,3).map(r=>`<span>• ${r}</span>`).join("")}</div>
    <div class="menu-actions alert-actions"><button onclick="resolveAlert('${a.id}','confirm')">This was me</button><button class="danger" onclick="resolveAlert('${a.id}','report')">Report fraud</button><button class="light" onclick="resolveAlert('${a.id}','freeze-card')">Freeze card</button></div>
  </article>`;
  if(status==="CONFIRMED_SAFE")return `<article class="fraud-alert-card alert-safe">
    <div class="resolved-alert-head"><div class="resolved-check">✓</div><div><small>REVIEW COMPLETE</small><h3>Confirmed as yours</h3></div></div>
    <div class="resolved-payment"><div><b>${a.merchantName}</b><small>${a.maskedCardNumber} · ${a.merchantCountry||"Location unavailable"}</small></div><strong>${currencyMoney(a.amount,a.currency)}</strong></div>
    <p>No action needed. Your card is ready to use.</p>
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
async function openCreateTrip(){
  const start=new Date(Date.now()+7*86400000).toISOString().slice(0,10);
  const end=new Date(Date.now()+14*86400000).toISOString().slice(0,10);
  openSheet(`<button class="back" onclick="activate('trips')">← My journeys</button>
    <span class="eyebrow">PLAN AHEAD</span><h2>Create a trip</h2>
    <p>Add your destination and choose the card you plan to use.</p>
    <form class="trip-form" id="tripForm">
      <label>DESTINATION COUNTRY / REGION
        <div class="trip-picker-wrap">
          <input class="trip-picker" id="countryInput" name="destinationCountry" placeholder="Search or choose country/region (e.g. Hong Kong, HK)" autocomplete="off" required>
          <div class="trip-picker-panel" id="countryPanel"></div>
        </div>
      </label>
      <label>CITY
        <div class="trip-picker-wrap">
          <input class="trip-picker" id="cityInput" name="destinationCity" placeholder="Search or choose city" autocomplete="off">
          <div class="trip-picker-panel" id="cityPanel"></div>
        </div>
      </label>
      <div class="row"><label>START DATE<input name="startDate" type="date" min="${new Date().toISOString().slice(0,10)}" value="${start}" required></label>
      <label>END DATE<input name="endDate" type="date" min="${new Date().toISOString().slice(0,10)}" value="${end}" required></label></div>
      <div class="row"><label>BUDGET<input name="budget" type="number" min="0.01" step="0.01" value="2500" required></label>
      <label>CURRENCY
        <div class="trip-picker-wrap">
          <input class="trip-picker" id="currencyInput" name="budgetCurrency" placeholder="Search or choose currency (e.g. HKD)" autocomplete="off" required>
          <div class="trip-picker-panel" id="currencyPanel"></div>
        </div>
      </label></div>
      <label>PREFERRED CARD<select name="preferredCardId" required>${cardOptions()}</select><small>The destination exchange rate is stored against this card when the journey is opened.</small></label>
      <button type="submit">Create trip</button>
    </form>`);
  await bindTripReferenceControls();
  $("#tripForm").onsubmit=createTrip;
}
async function createTrip(event){
  event.preventDefault();
  const form=new FormData(event.currentTarget);
  const payload=Object.fromEntries(form.entries());
  payload.budget=Number(payload.budget);
  payload.destinationCountry=resolveCountrySelection(payload.destinationCountry)?.name||payload.destinationCountry.trim();
  payload.budgetCurrency=parseCurrencyCode(payload.budgetCurrency);
  if(payload.endDate<payload.startDate){toast("End date must be on or after the start date");return}
  const button=event.currentTarget.querySelector("button");button.disabled=true;button.textContent="Creating…";
  try{
    const trip=await api("/api/travel/trips",{method:"POST",body:JSON.stringify(payload)});
    await load();
    openSheet(`<div class="success-panel"><b>Trip created</b><p>${trip.destinationCity||trip.destinationCountry}, ${trip.destinationCountry} has been added to your journeys.</p></div>
      <div class="menu-actions"><button onclick="showJourney('${trip.id}')">Open journey</button><button class="light" onclick="runCheck('${trip.id}')">Run readiness check</button></div>`);
  }catch(e){toast(e.message);button.disabled=false;button.textContent="Create trip"}
}
window.openTripEditor=id=>{
  const t=state.trips.find(x=>x.id===id);if(!t||t.status!=="PLANNED")return;
  openSheet(`<button class="back" onclick="showJourney('${id}')">← Trip details</button>
    <span class="eyebrow">EDIT PLANNED TRIP</span><h2>Update your trip</h2>
    <form class="trip-form" id="editTripForm">
      <label>DESTINATION COUNTRY / REGION
        <div class="trip-picker-wrap">
          <input class="trip-picker" id="countryInput" name="destinationCountry" placeholder="Search or choose country/region" autocomplete="off" value="${escapeHtml(t.destinationCountry)}" required>
          <div class="trip-picker-panel" id="countryPanel"></div>
        </div>
      </label>
      <label>CITY
        <div class="trip-picker-wrap">
          <input class="trip-picker" id="cityInput" name="destinationCity" placeholder="Search or choose city" autocomplete="off" value="${escapeHtml(t.destinationCity||"")}">
          <div class="trip-picker-panel" id="cityPanel"></div>
        </div>
      </label>
      <div class="row"><label>START DATE<input name="startDate" type="date" min="${new Date().toISOString().slice(0,10)}" value="${t.startDate}" required></label>
      <label>END DATE<input name="endDate" type="date" min="${new Date().toISOString().slice(0,10)}" value="${t.endDate}" required></label></div>
      <div class="row"><label>BUDGET<input name="budget" type="number" min="0.01" step="0.01" value="${t.budget}" required></label>
      <label>CURRENCY
        <div class="trip-picker-wrap">
          <input class="trip-picker" id="currencyInput" name="budgetCurrency" placeholder="Search or choose currency" autocomplete="off" value="${escapeHtml(t.budgetCurrency)}" required>
          <div class="trip-picker-panel" id="currencyPanel"></div>
        </div>
      </label></div>
      <label>PREFERRED CARD<select name="preferredCardId" required>${state.cards.filter(c=>c.status==="ACTIVE").map(c=>`<option value="${c.id}" ${c.id===t.preferredCardId?"selected":""}>${c.cardType} ${c.maskedCardNumber} · ${c.mainCurrency}</option>`).join("")}</select></label>
      <button type="submit">Save trip changes</button>
    </form>`);
  bindTripReferenceControls({countryValue:t.destinationCountry,cityValue:t.destinationCity||"",currencyValue:t.budgetCurrency}).then(()=>{
    $("#editTripForm").onsubmit=e=>updateTrip(e,id);
  });
};
async function updateTrip(event,id){
  event.preventDefault();const form=new FormData(event.currentTarget),payload=Object.fromEntries(form.entries());
  payload.budget=Number(payload.budget);payload.budgetCurrency=parseCurrencyCode(payload.budgetCurrency);
  if(payload.endDate<payload.startDate){toast("End date must be on or after the start date");return}
  const button=event.currentTarget.querySelector("button");button.disabled=true;button.textContent="Saving…";
  try{await api(`/api/travel/trips/${id}`,{method:"PUT",body:JSON.stringify(payload)});await load();toast("Trip updated");showJourney(id)}
  catch(e){toast(e.message);button.disabled=false;button.textContent="Save trip changes"}
}
window.confirmDeleteTrip=id=>{
  const t=state.trips.find(x=>x.id===id);if(!t||t.status!=="PLANNED")return;
  openSheet(`<button class="back" onclick="showJourney('${id}')">← Trip details</button><span class="eyebrow">DELETE PLANNED TRIP</span>
    <h2>Delete ${escapeHtml(t.destinationCity||t.destinationCountry)}?</h2>
    <p>This removes the travel plan, readiness results and saved cash plan. Card transactions are not affected.</p>
    <div class="delete-warning"><b>${escapeHtml(t.destinationCity||t.destinationCountry)}, ${escapeHtml(t.destinationCountry)}</b><small>${t.startDate} — ${t.endDate}</small></div>
    <button class="bank-danger" onclick="deleteTrip('${id}')">Delete trip</button><button class="bank-secondary" onclick="showJourney('${id}')">Keep trip</button>`);
};
window.deleteTrip=async id=>{
  try{await api(`/api/travel/trips/${id}`,{method:"DELETE"});await load();toast("Planned trip deleted");activate("trips")}
  catch(e){toast(e.message)}
};

async function load(){
  try{
    updateGreeting();
    const tripsResponse=await api("/api/travel/trips");
    const trips=Array.isArray(tripsResponse?.data)?tripsResponse.data:[];
    trips.sort((a,b)=>(a.status==="COMPLETED")-(b.status==="COMPLETED")||a.startDate.localeCompare(b.startDate));

    if(!trips.length){
      state={
        trips:[],dashboard:{budget:0,spent:0,remaining:0,recentTransactions:[],currency:"USD",readinessStatus:"NOT_READY"},
        alerts:[],cards:[],transactions:[],cases:[],tripMoney:{},tripReadiness:{}
      };
      $("#trips").innerHTML='<p class="empty">No trips yet. Tap "New trip" to start planning.</p>';
      $("#spent").textContent=money(0);
      $("#remaining").innerHTML=money(0);
      $("#budget").textContent=money(0);
      $("#paymentIssue").innerHTML="";
      $("#caseTracking").innerHTML="";
      $("#attentionSection").style.display="none";
      renderHomeAssistant(null);
      renderReadinessTodoStack();
      renderReadinessIssueStack();
      maybeShowFraudAlert();
      return;
    }

    const focusTrip=trips.find(t=>t.status!=="COMPLETED"&&t.status!=="CANCELLED")||trips[0];
    const [dashboardResp,focusFx,alertsResp,cardsResp,transactionsResp,casesResp]=await Promise.all([
      api(`/api/travel/trips/${focusTrip.id}/dashboard`),
      api(`/api/travel/trips/${focusTrip.id}/exchange-rate?_=${Date.now()}`,{headers:{"Cache-Control":"no-cache"}}),
      api("/api/travel/alerts"),api("/api/travel/cards"),api("/api/travel/transactions"),api("/api/travel/cases")
    ]);
    const dashboard={
      budget:0,spent:0,remaining:0,recentTransactions:[],currency:"USD",readinessStatus:"NOT_READY",
      ...(dashboardResp||{})
    };
    dashboard.recentTransactions=Array.isArray(dashboard.recentTransactions)?dashboard.recentTransactions:[];
    const alerts=Array.isArray(alertsResp?.data)?alertsResp.data:[];
    const cards=Array.isArray(cardsResp?.data)?cardsResp.data:[];
    const transactions=Array.isArray(transactionsResp?.data)?transactionsResp.data:[];
    const cases=Array.isArray(casesResp?.data)?casesResp.data:[];

    const upcoming=trips.filter(t=>t.status!=="COMPLETED"&&t.status!=="CANCELLED");
    const tripMoney=Object.fromEntries(await Promise.all(upcoming.map(async t=>{
      if(t.id===focusTrip.id)return [t.id,{dashboard,fx:focusFx}];
      try{const [d,fx]=await Promise.all([api(`/api/travel/trips/${t.id}/dashboard`),api(`/api/travel/trips/${t.id}/exchange-rate?_=${Date.now()}`,{headers:{"Cache-Control":"no-cache"}})]);return [t.id,{dashboard:d,fx}]}
      catch(e){return [t.id,null]}
    })));
    const displayedTrips=upcoming.length?upcoming:trips;
    const tripReadiness=Object.fromEntries(await Promise.all(displayedTrips.map(async t=>{
      try{return [t.id,await api(`/api/travel/trips/${t.id}/readiness`)];}
      catch{return [t.id,null];}
    })));
    state={trips,dashboard,alerts,cards,transactions,cases,tripMoney,tripReadiness};
    $("#trips").innerHTML=(displayedTrips.length?displayedTrips.map(t=>`<article class="trip" role="button" tabindex="0" onclick="showJourney('${t.id}')">
      <span class="tag">${t.status}</span><span class="arrow">→</span>
      <h3>${t.destinationCity}, ${t.destinationCountry}</h3>
      <p>${t.startDate} — ${t.endDate} · ${currencyMoney(t.budget,t.budgetCurrency)}</p>
      ${readinessBadge(t.id)}
      ${state.tripMoney[t.id]?`<small class="trip-local">Remaining ${localRemaining(state.tripMoney[t.id].dashboard,state.tripMoney[t.id].fx)}</small>`:""}
    </article>`).join(""):'<p class="empty">No upcoming journeys yet.</p>');
    const displayCurrency=(focusTrip.budgetCurrency||dashboard.currency||"USD").toUpperCase();
    const displayDashboard={...dashboard,currency:displayCurrency};
    $("#spent").textContent=currencyMoney(dashboard.spent,displayCurrency);
    const convertedRemaining=localRemaining(displayDashboard,focusFx,true);
    $("#remaining").innerHTML=`${currencyMoney(dashboard.remaining,displayCurrency)}${convertedRemaining?`<small>${convertedRemaining}</small>`:""}`;
    $("#budget").textContent=currencyMoney(dashboard.budget,displayCurrency);

    const failed=dashboard.recentTransactions.find(t=>t.status==="DECLINED"&&t.failureCode==="NETWORK_ERROR")||dashboard.recentTransactions.find(t=>t.status==="DECLINED");
    const featuredByHero=renderHomeAssistant(failed);
    $("#attentionSection").style.display=failed&&!featuredByHero?"block":"none";
    $("#paymentIssue").innerHTML=failed?`<div class="issue-card interrupted-home" onclick="paymentHelp('${failed.transactionId}')">
      <div class="issue-icon">!</div><div><small>PAYMENT INTERRUPTED</small><h3>Complete your ${failed.merchantName} payment</h3><p>${currencyMoney(failed.billingAmount,failed.billingCurrency)} · We found the best next step</p></div><span class="chevron">→</span></div>`:"";
    const activeCases=cases.filter(c=>c.status!=="RESOLVED");
    const featuredCase=activeCases[0]||cases[0];
    $("#caseTracking").innerHTML=featuredCase?`<div class="section-head compact-head tracking-heading"><h2>Case tracking</h2><button class="icon-action" onclick="showCases()" aria-label="View all cases" title="View all cases"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M5 12h14M14 7l5 5-5 5"/></svg></button></div>${caseCard(featuredCase)}`:"";
    renderReadinessTodoStack();
    renderReadinessIssueStack();
    maybeShowFraudAlert();
  }catch(e){toast(e.message)}
}
function localRemaining(d,fx,approx=false){
  if(!fx?.rate||!fx.destinationCurrency)return "";
  const sourceCurrency=(d.currency||"USD").toUpperCase();
  if(sourceCurrency==="USD")return "";
  const cardCurrency=(fx.cardCurrency||"").toUpperCase();
  const destinationCurrency=fx.destinationCurrency.toUpperCase();
  let usd;
  if(cardCurrency==="USD"&&destinationCurrency===sourceCurrency)usd=Number(d.remaining)/Number(fx.rate);
  else if(cardCurrency===sourceCurrency&&destinationCurrency==="USD")usd=Number(d.remaining)*Number(fx.rate);
  else return "";
  return `${approx?"≈ ":""}${currencyMoney(usd,"USD")} USD`;
}
function updateGreeting(){
  const hour=new Date().getHours();
  const greeting=hour<5?"Welcome back":hour<12?"Morning":hour<18?"Afternoon":"Evening";
  $("#welcomeTitle").textContent=`${greeting}, Jessie.`;
}
const caseCard=c=>`<div class="case-card ${c.status==="RESOLVED"?"case-resolved":""}" onclick="showCase('${c.id}')" role="button"><div class="case-top"><span>${c.status.replaceAll("_"," ")}</span><b>${c.id}</b></div><h3>${c.title}</h3><p>${c.currentUpdate}</p><small>Updated ${new Date(c.updatedAt).toLocaleString()}</small><div class="case-progress"><i></i><i class="${c.status!=="SUBMITTED"?"done":""}"></i><i class="${c.status==="RESOLVED"?"done":""}"></i></div></div>`;
window.showCases=()=>{
  const cases=uniqueByTransaction(state.cases);
  const active=cases.filter(c=>c.status!=="RESOLVED"),closed=cases.filter(c=>c.status==="RESOLVED");
  openSheet(`<button class="back" onclick="activate('home')">← Overview</button><span class="eyebrow">CASE TRACKING</span><h2>Bank cases</h2>
    ${active.length?`<h3>In progress</h3>${active.map(caseCard).join("")}`:""}
    ${closed.length?`<h3 class="case-section-title">Completed</h3>${closed.map(caseCard).join("")}`:""}
    ${state.cases.length?"":"<p>No support cases.</p>"}`);
};
window.showCase=id=>{
  const c=state.cases.find(x=>x.id===id);if(!c)return;
  const fraud=c.type==="FRAUD_INVESTIGATION";
  openSheet(`<button class="back" onclick="showCases()">← All cases</button><span class="eyebrow">${fraud?"FRAUD INVESTIGATION":c.type.replaceAll("_"," ")}</span><h2>${c.title}</h2><div class="case-reference"><small>CASE REFERENCE</small><b>${c.id}</b><span>${c.status.replaceAll("_"," ")}</span></div>
    <div class="case-timeline">
      <div class="done"><b>${fraud?"Fraud reported":"Request received"}</b><small>${new Date(c.createdAt).toLocaleString()}</small></div>
      ${fraud?`<div class="done"><b>Payment protected</b><small>The payment was disputed and the card was secured.</small></div>`:""}
      <div class="${c.status!=="SUBMITTED"?"done":""}"><b>Bank investigation</b><small>${c.status==="RESOLVED"?"Merchant and payment-network evidence reviewed.":c.currentUpdate}</small></div>
      <div class="${c.status==="RESOLVED"?"done":""}"><b>Case outcome</b><small>${c.status==="RESOLVED"?c.currentUpdate:"We’ll notify you when the investigation is complete."}</small></div>
    </div><button class="bank-secondary" onclick="activate('home')">Back to overview</button>`);
};
function renderHomeAssistant(failed){
  const next=state.trips.find(t=>t.status!=="COMPLETED"&&t.status!=="CANCELLED")||state.trips[0];
  const panel=$("#nextBestAction");
  if(!panel)return false;
  if(!next){panel.hidden=true;return false}
  const openAlert=state.alerts.find(a=>a.status==="OPEN");
  const preferred=state.cards.find(c=>c.id===next.preferredCardId);
  let title="",detail="",button="",action=null,score="!",featuredPayment=false;
  if(openAlert){title="Confirm an unusual travel payment";detail=`Check ${openAlert.merchantName}, ${currencyMoney(openAlert.amount,openAlert.currency)} before we take action.`;button="Review payment";action=showAlerts}
  else if(failed){title="Resolve your declined payment";detail=`We found the likely reason ${failed.merchantName} was declined and prepared the next step.`;button="Fix payment";action=()=>paymentHelp(failed.transactionId);featuredPayment=true}
  else if(preferred&&!preferred.overseasPaymentsEnabled){title="Turn on overseas payments";detail=`Your preferred ${preferred.cardType} ${preferred.maskedCardNumber} is not yet enabled abroad.`;button="Enable securely";action=()=>cardAction(preferred.id,"enable-overseas-payments")}
  panel.hidden=!action;
  if(!action)return false;
  $("#assistantAction").textContent=title;$("#readinessText").textContent=detail;$("#readinessText").hidden=!detail;$("#assistantButton").textContent=button;$("#score").textContent=score;$("#checkBtn").onclick=action;
  return featuredPayment;
}
async function runCheck(id="trip-tokyo"){
  try{
    const r=await api(`/api/travel/trips/${id}/readiness-check`,{method:"POST"});
    state.tripReadiness={...(state.tripReadiness||{}),[id]:r};
    if(r.status==="READY")dismissReadinessTodo(id,false);else{renderReadinessTodoStack();renderReadinessIssueStack();}
    if(id==="trip-tokyo"){
      $("#score").textContent=r.score+"/100";$("#ring span").textContent=r.score;
      $("#readinessText").hidden=false;$("#readinessText").textContent=r.status==="READY"?"Everything is ready. Have a great trip!":"A few settings need attention before you leave.";
    }
    openReadinessSheet(id,r);
  }catch(e){toast(e.message)}
}
function openReadinessSheet(id,r){
  openSheet(`<button class="back" onclick="showJourney('${id}')">← Journey details</button>
      <h2>Readiness · ${r.score}/100</h2>
      ${r.checks.map(c=>`<div class="check ${c.passed?"":"fail"}"><b>${c.passed?"✓":"!"} ${c.message}</b>${c.recommendedAction?`<small>${c.recommendedAction}</small>`:""}${c.passed?"":readinessAction(id,c.ruleCode)}</div>`).join("")}`);
}
window.runReadinessTodo=async tripId=>{
  try{
    const r=await api(`/api/travel/trips/${tripId}/readiness-check`,{method:"POST"});
    state.tripReadiness={...(state.tripReadiness||{}),[tripId]:r};
    if(r.status==="READY"){
      dismissReadinessTodo(tripId);
      return;
    }
    renderReadinessIssueStack();
    openReadinessSheet(tripId,r);
  }catch(e){toast(e.message)}
};
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
  try{
    await api(`/api/travel/trips/${tripId}`,{method:"PUT",body:JSON.stringify({destinationCountry:t.destinationCountry,destinationCity:t.destinationCity,startDate:t.startDate,endDate:t.endDate,budget:t.budget,budgetCurrency:t.budgetCurrency,preferredCardId:cardId})});
    const readiness=await runReadinessCheckSilently(tripId);
    updateReadinessAfterFix(tripId,readiness);
    toast(currencyCheckPassed(readiness)?"Check passed: currency support is now confirmed.":"Preferred card updated");
    showJourney(tripId);
  }catch(e){toast(e.message)}
};
async function applyCurrencyFallback(tripId,option,openAtm=false){
  try{
    await api(`/api/travel/trips/${tripId}/readiness/currency-fallback`,{method:"POST",body:JSON.stringify({option})});
    const readiness=await runReadinessCheckSilently(tripId);
    updateReadinessAfterFix(tripId,readiness);
    toast(currencyCheckPassed(readiness)?"Check passed: currency handling is now complete.":(option==="USD_SETTLEMENT"?"USD settlement accepted for this trip":"ATM cash withdrawal selected"));
    if(openAtm){
      planCashExchange(tripId);
      return;
    }
    runCheck(tripId);
  }catch(e){toast(e.message)}
}
function showCurrencyGuidance(tripId,ruleCode="CURRENCY_UNKNOWN"){
  openSheet(`<button class="back" onclick="runCheck('${tripId}')">← Readiness check</button><span class="eyebrow">CURRENCY SUPPORT</span>
    <h2>${ruleCode==="CURRENCY_NOT_SUPPORTED"?"Currency handling needed":"Destination currency not verified"}</h2>
    <p>If this card does not support the local currency, choose how to proceed for this trip.</p>
    <button class="bank-primary" onclick="showCardSolution('${tripId}','${ruleCode}')">Switch to a supporting card</button>
    <button class="bank-secondary" onclick="applyCurrencyFallback('${tripId}','USD_SETTLEMENT')">Accept USD settlement</button>
    <button class="bank-secondary" onclick="applyCurrencyFallback('${tripId}','ATM_CASH',true)">Use ATM cash instead of USD settlement</button>`);
}
window.planCashExchange=(tripId,origin="trip")=>{
  const trip=state.trips.find(t=>t.id===tripId);if(!trip)return;
  activeAtmTripId=tripId;
  const back=origin==="overview"?`activate('home')`:`showJourney('${tripId}')`;
  const backLabel=origin==="overview"?"Overview":"Trip details";
  openSheet(`<button class="back" onclick="${back}">← ${backLabel}</button><span class="eyebrow">ATM FINDER</span><h2>Find an ATM</h2>
    <p>Search near an address, airport or hotel.</p>
    <div class="atm-search"><label>FIND NEARBY ATMS</label><div><input id="atmLocationQuery" value="${trip.destinationCity||trip.destinationCountry}, ${trip.destinationCountry}" onkeydown="if(event.key==='Enter'){event.preventDefault();searchNearbyAtms('${tripId}')}"><button id="atmSearchButton" type="button" onclick="searchNearbyAtms('${tripId}')">Search</button></div><button class="location-button" type="button" onclick="useCurrentLocationForAtms('${tripId}')">◎ Use my location</button></div>
    <div id="atmMap" class="atm-map"><div>Search a location to view nearby ATMs</div></div>
    <div id="atmResults" class="atm-results"><div class="atm-empty">Enter an address, airport or hotel above.</div></div>`);
};
window.searchNearbyAtms=async tripId=>{
  if(atmSearchInFlight)return;
  const query=$("#atmLocationQuery")?.value.trim();if(!query)return;
  atmSearchInFlight=true;const button=$("#atmSearchButton");if(button){button.disabled=true;button.textContent="Searching…"}
  setAtmLoading("Finding this location…");
  try{
    const location=await api(`/api/travel/maps/geocode?q=${encodeURIComponent(query)}`);
    await loadNearbyAtms(location.latitude,location.longitude,location.label);
  }catch(e){showAtmError(e.message==="Unexpected server error"?"Location search is temporarily unavailable. Please try again.":e.message)}
  finally{atmSearchInFlight=false;if(button){button.disabled=false;button.textContent="Search"}}
};
window.useCurrentLocationForAtms=tripId=>{
  if(!navigator.geolocation){showAtmError("Location access is not available.");return}
  setAtmLoading("Getting your location…");
  navigator.geolocation.getCurrentPosition(
    p=>loadNearbyAtms(p.coords.latitude,p.coords.longitude,"Current location"),
    ()=>showAtmError("Location access was not allowed. Search an address instead."),
    {enableHighAccuracy:true,timeout:10000,maximumAge:60000});
};
async function loadNearbyAtms(lat,lon,label){
  try{
    const result=await api(`/api/travel/maps/atms?lat=${lat}&lon=${lon}&radius=2500`);
    currentAtms=result.atms||[];
    renderAtmMap(lat,lon,result.atms);
    $("#atmResults").innerHTML=result.atms.length?`<div class="atm-results-head"><b>${result.atms.length} near ${escapeHtml(shortPlace(label))}</b><small>Tap to view</small></div>${result.atms.slice(0,6).map(atmCard).join("")}`:`<div class="atm-empty">No mapped ATM found. Try another location.</div>`;
  }catch(e){showAtmError("ATM search is temporarily unavailable. Please try again.")}
}
function atmCard(atm,index){
  const distance=atm.distanceMeters<1000?`${atm.distanceMeters} m`:`${(atm.distanceMeters/1000).toFixed(1)} km`;
  return `<button type="button" class="atm-result" onclick="selectAtmByIndex(${index})"><span><b>${escapeHtml(atm.name)}</b><small>${escapeHtml(atm.address)}${atm.openingHours?` · ${escapeHtml(atm.openingHours)}`:""}</small></span><strong>${distance}</strong></button>`;
}
function renderAtmMap(lat,lon,atms){
  if(activeAtmMap){activeAtmMap.remove();activeAtmMap=null}
  if(!window.L){$("#atmMap").innerHTML="<div>Map unavailable. ATM results are listed below.</div>";return}
  $("#atmMap").innerHTML="";
  activeAtmMap=L.map("atmMap",{zoomControl:false}).setView([lat,lon],14);
  L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png",{maxZoom:19,attribution:"© OpenStreetMap contributors"}).addTo(activeAtmMap);
  L.control.zoom({position:"bottomright"}).addTo(activeAtmMap);
  L.circleMarker([lat,lon],{radius:8,color:"#0d6b4b",fillColor:"#d5f16b",fillOpacity:1,weight:3}).addTo(activeAtmMap).bindPopup("Search location");
  atms.forEach(atm=>L.marker([atm.latitude,atm.longitude]).addTo(activeAtmMap).bindPopup(`<b>${escapeHtml(atm.name)}</b><br>${escapeHtml(atm.address)}`));
}
window.selectAtmByIndex=async index=>{
  const atm=currentAtms[index];if(!atm)return;
  if(activeAtmMap)activeAtmMap.setView([atm.latitude,atm.longitude],16);
  toast(`${atm.name} shown on map`);
  if(activeAtmTripId){
    const readiness=await runReadinessCheckSilently(activeAtmTripId);
    updateReadinessAfterFix(activeAtmTripId,readiness);
    if(currencyCheckPassed(readiness))toast("Check passed: ATM cash plan has been recorded.");
  }
};
function setAtmLoading(message){if($("#atmResults"))$("#atmResults").innerHTML=`<div class="atm-empty">${message}</div>`}
function showAtmError(message){if($("#atmResults"))$("#atmResults").innerHTML=`<div class="atm-empty error">${escapeHtml(message)}</div>`}
function shortPlace(label){return label.split(",").slice(0,3).join(",")}
function escapeHtml(value){return String(value||"").replace(/[&<>"']/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;","\"":"&quot;","'":"&#39;"}[c]))}
window.showJourney=async(id,origin)=>{
  try{
    activeJourneyOrigin=normalizeOverviewOrigin(origin||activeRootPage);
    const backLabel=overviewBackLabel(activeJourneyOrigin);
    const backAction=overviewBackAction(activeJourneyOrigin);
    const trip=state.trips.find(t=>t.id===id),[d,fx]=await Promise.all([api(`/api/travel/trips/${id}/dashboard`),api(`/api/travel/trips/${id}/exchange-rate?_=${Date.now()}`,{headers:{"Cache-Control":"no-cache"}})]);
    const card=state.cards.find(c=>c.id===trip.preferredCardId),used=Number(d.budgetUsagePercentage||0);
    const budgetAdvice=used>100?"Spending is above the planned budget. Review recent purchases before using more funds.":used>75?"Most of the budget has been used. Keep a closer eye on the remaining days.":used>0?`${Math.round(used)}% of the budget has been used and ${currencyMoney(d.remaining,d.currency)} remains.`:"No spending yet. Your full travel budget is still available.";
    const cardAdvice=fx.cardSupportsCurrency?`${card?.cardType||"Your card"} ${fx.maskedCardNumber} supports ${fx.destinationCurrency} and overseas payments are ${card?.overseasPaymentsEnabled?"enabled":"not enabled"}.`:`${card?.cardType||"Your card"} ${fx.maskedCardNumber} is not verified for this destination currency.`;
    const remainingLocal=localRemaining(d,fx,true);
    const planned=trip.status==="PLANNED";
    openSheet(`<button class="back" onclick="${backAction}">← ${backLabel}</button>
      <span class="eyebrow">${trip.status}</span><h2>${trip.destinationCity}, ${trip.destinationCountry}</h2>
      <p>${trip.startDate} — ${trip.endDate}</p>
      ${planned?`<div class="trip-manage"><button onclick="openTripEditor('${id}')">Edit trip</button><button class="danger-link" onclick="confirmDeleteTrip('${id}')">Delete</button></div>`:""}
      <div class="detail-grid">

        <div><small>BUDGET</small><strong>${currencyMoney(d.budget,d.currency)}</strong></div>
        <div><small>REMAINING</small><strong>${currencyMoney(d.remaining,d.currency)}<em>${remainingLocal}</em></strong></div>
        <div><small>SPENT</small><strong>${currencyMoney(d.spent,d.currency)}</strong></div>

        <div><small>TRANSACTIONS</small><strong>${d.transactionCount}</strong></div>
      </div>
      ${trip.status==="COMPLETED"?renderTripRecap(d,trip):""}
      ${planned?`<button class="budget-edit" onclick="openBudgetEditor('${id}')">Adjust travel budget <span>→</span></button>`:""}
      <div class="journey-fx ${fx.cardSupportsCurrency?"":"fx-warning"}"><div class="fx-top"><div><small>PREFERRED CARD · ${fx.maskedCardNumber}</small><strong>${fx.destinationCurrency&&fx.rate?`1 ${fx.cardCurrency} = ${Number(fx.rate).toFixed(4)} ${fx.destinationCurrency}`:"Rate unavailable"}</strong></div><button onclick="showJourney('${id}')">↻</button></div>
      <p>${!fx.destinationCurrencyVerified?"Currency unavailable.":fx.cardSupportsCurrency?`Ready for ${fx.destinationCurrency}.`:`${fx.destinationCurrency} isn’t supported by this card.`}</p>
      ${!fx.destinationCurrencyVerified||!fx.cardSupportsCurrency?`<div class="menu-actions"><button onclick="showCardSolution('${id}','${fx.destinationCurrencyVerified?"CURRENCY_NOT_SUPPORTED":"CURRENCY_UNKNOWN"}')">Switch card</button><button class="light" onclick="planCashExchange('${id}')">ATM</button></div>`:""}</div>
      <div class="menu-actions">${planned?`<button onclick="runCheck('${id}')">Run readiness check</button>`:""}<button class="light" onclick="showJourneyPayments('${id}')">View payments</button>${planned?`<button class="light" onclick="showCardSolution('${id}','CHANGE_CARD')">Change preferred card</button>`:""}</div>
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
  const fraudAlert=state.alerts?.find(a=>a.transactionId===t.transactionId&&a.status==="REPORTED_FRAUD");
  const label=fraudAlert?"⚑ Reported as fraud · Bank investigating":recovered?"✓ Paid after retry":interrupted?"● Payment interrupted · Action needed":t.status;
  return `<div class="check ${fraudAlert?"fraud-reported":interrupted?"interrupted":recovered?"recovered":""}"><b>${t.merchantName} · ${currencyMoney(t.billingAmount,t.billingCurrency)}</b>
    <small>${t.transactionTime.slice(0,10)} · ${label}</small>
    ${fraudAlert?`<button class="inline-link" onclick="showFraudCaseForTransaction('${t.transactionId}')">Track fraud case →</button>`:interrupted||recovered?`<button class="inline-link" onclick="paymentHelp('${t.transactionId}')">${recovered?"View recovery timeline":"Complete this payment"} →</button>`:""}</div>`;
}
window.showFraudCaseForTransaction=transactionId=>{
  const fraudCase=state.cases.find(c=>c.transactionId===transactionId&&c.type==="FRAUD_INVESTIGATION");
  if(fraudCase)showCase(fraudCase.id);else showAlerts();
};
window.showJourneyPayments=async id=>{
  try{const d=await api(`/api/travel/trips/${id}/dashboard`);openSheet(`<button class="back" onclick="showJourney('${id}')">← Journey</button><h2>Journey payments</h2>${d.recentTransactions.length?d.recentTransactions.map(transactionRow).join(""):"<p>No transactions yet.</p>"}`)}
  catch(e){toast(e.message)}
};
window.paymentHelp=async id=>{
  try{
    const r=await api(`/api/travel/transactions/${id}/recovery`);
    activeRecovery=r;
    const complete=r.status.startsWith("COMPLETED_"),securityCleared=r.status==="SECURITY_CLEARED_RETRY_REQUIRED";
    const primary=complete?`<button class="bank-primary" onclick="activate('transactions')">Done</button>`:
      r.recommendedAction==="RETRY_PAYMENT"?`<button class="bank-primary recovery-primary" onclick="confirmRecoveryRetry('${id}')">Retry payment safely</button>`:
      r.recommendedAction==="CHANGE_LIMIT"?`<div class="recovery-choice-actions"><button class="bank-primary" onclick="increaseLimit('${id}')">Increase limit & retry</button><button class="bank-secondary" onclick="useAnotherCard('${id}')">Use another card</button></div>`:
      r.recommendedAction==="ADD_FUNDS"?`<button class="bank-primary" onclick="fundingGuidance()">View funding options</button>`:
      r.recommendedAction==="REVIEW_SECURITY"?`<button class="bank-primary" onclick="showAlerts()">Review security alert</button>`:
      r.recommendedAction==="RETRY_AT_MERCHANT"?`<button class="bank-primary" onclick="markReadyAtMerchant('${id}')">I’ll retry at the merchant</button>`:
      `<button class="bank-primary" onclick="secureSupport('${id}')">Contact secure support</button>`;
    openSheet(`<button class="back" onclick="activate('transactions')">← Transactions</button><span class="eyebrow">PAYMENT HELP</span><h2>${complete?"Payment completed":"Complete this payment"}</h2>
      <div class="recovery-payment ${complete?"complete":securityCleared?"cleared":""}"><div><small>${complete?"PAID AFTER RETRY":securityCleared?"SECURITY CHECK COMPLETE":"PAYMENT INTERRUPTED"}</small><h3>${currencyMoney(r.amount,r.currency)}</h3><p>${r.merchantName} · ${r.merchantCity}, ${r.merchantCountry}<br>${r.maskedCardNumber}</p></div><span>${complete?"✓":securityCleared?"✓":"!"}</span></div>
      <div class="status-explainer"><b>${complete?"Only one payment was made":"No money was taken"}</b></div>
      <section class="ai-decision"><div class="ai-title"><span>✦</span><div><small>PAYMENT CHECK</small><h3>${r.explanation}</h3></div></div>
        <div class="recovery-checks">${r.checks.map(c=>`<div class="${c.passed?"pass":"warn"}"><i>${c.passed?"✓":"!"}</i><span><b>${c.label}</b><small>${c.detail}</small></span></div>`).join("")}</div>
      </section>
      ${complete?recoveryTimeline(r.timeline):`<div class="best-next ${securityCleared?"cleared":""}"><small>BEST NEXT STEP</small><h3>${r.recommendedAction==="RETRY_PAYMENT"?"Retry this payment now":r.recommendedAction==="RETRY_AT_MERCHANT"?"Retry once at the merchant":"Complete the required action"}</h3><p>${r.recommendedAction==="RETRY_PAYMENT"?"We’ll send a new authorization request. The previous interrupted attempt cannot be charged twice.":r.recommendedAction==="RETRY_AT_MERCHANT"?"You confirmed the payment. Ask the merchant to run your card again; the earlier declined attempt will not be charged.":"Resolve the issue below, then return to complete the payment."}</p></div>`}
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
window.increaseLimit=async(transactionId=activeRecovery?.transactionId)=>{
  if(!transactionId){toast("Open a payment recovery first");return}
  const recovery=activeRecovery?.transactionId===transactionId?activeRecovery:await api(`/api/travel/transactions/${transactionId}/recovery`);
  activeRecovery=recovery;
  const card=state.cards.find(c=>c.id===recovery.cardId);
  const newLimit=Math.max(Math.ceil(Number(recovery.amount)/500)*500,Number(card?.dailyPaymentLimit||0)+500);
  secureCardRequest("PAYMENT_LIMIT_CHANGE",recovery.cardId,`/api/travel/cards/${recovery.cardId}/payment-limit`,
    {method:"PUT",body:JSON.stringify({newLimit})},()=>openSheet(`<div class="success-panel"><b>Payment limit updated</b><p>${card?.cardType||"Card"} ${recovery.maskedCardNumber} daily payment limit is now ${currencyMoney(newLimit,recovery.currency)}.</p></div><button class="bank-primary" onclick="confirmRecoveryRetry('${transactionId}')">Retry payment now</button><button class="bank-secondary" onclick="useAnotherCard('${transactionId}')">Use another card instead</button>`));
};
window.markReadyAtMerchant=id=>openSheet(`<button class="back" onclick="paymentHelp('${id}')">← Payment recovery</button><span class="eyebrow">READY TO TRY AGAIN</span><h2>Please ask the merchant to swipe or tap your card again</h2><div class="success-panel neutral"><b>Security check complete</b><p>The payment was confirmed as yours. The original attempt was declined, so only the new in-person attempt can be charged.</p></div><button class="bank-primary" onclick="activate('transactions')">Done</button>`);
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
  activeRootPage=page;
  document.querySelectorAll("nav button").forEach(b=>b.classList.toggle("active",b.dataset.page===page));
  if(page==="home"){
    $("#contentPage").classList.add("hidden");
    $("#homePage").classList.remove("hidden");
    $("#sheet").classList.add("hidden");
    renderReadinessTodoStack();
    renderReadinessIssueStack();
    $("#homePage").scrollTo({top:0,behavior:"smooth"});
    return;
  }
  if(page==="trips"){
    const planned=state.trips.filter(t=>t.status==="PLANNED"||t.status==="ACTIVE");
    const history=state.trips.filter(t=>t.status==="COMPLETED")
      .sort((a,b)=>b.endDate.localeCompare(a.endDate)||b.startDate.localeCompare(a.startDate));
    openSheet(`<span class="eyebrow">YOUR JOURNEYS</span><h2>My trips</h2><p>Plan what’s next and revisit previous destinations.</p>
      <div class="menu-actions"><button onclick="openCreateTrip()">+ Create a trip</button></div>
      <h3>Planned</h3>${planned.length?planned.map(journeyCard).join(""):"<p>No planned trips.</p>"}
      ${history.length?`<details class="past-trips"><summary>Past trips <span>${history.length}</span></summary>${history.map(journeyCard).join("")}</details>`:""}`);
  }
  if(page==="transactions")renderTransactionsPage();
  if(page==="profile")renderProfilePage();
}
const visitedCoordinates={
  "Singapore":[1.3521,103.8198],"New York":[40.7128,-74.0060],"Barcelona":[41.3874,2.1686],
  "Sydney":[-33.8688,151.2093],"Tokyo":[35.6762,139.6503],"Paris":[48.8566,2.3522],
  "Toronto":[43.6532,-79.3832],"Nice":[43.7102,7.2620],"London":[51.5072,-0.1276],
  "Rome":[41.9028,12.4964],"Dubai":[25.2048,55.2708],"Cape Town":[-33.9249,18.4241],
  "Rio de Janeiro":[-22.9068,-43.1729],"Vancouver":[49.2827,-123.1207],
  "Bangkok":[13.7563,100.5018],"Reykjavik":[64.1466,-21.9426],"Mexico City":[19.4326,-99.1332],
  "Japan":[36.2048,138.2529],"France":[46.2276,2.2137],"Canada":[56.1304,-106.3468],
  "United States":[37.0902,-95.7129],"Spain":[40.4637,-3.7492],"Australia":[-25.2744,133.7751],
  "Italy":[41.8719,12.5674],"United Arab Emirates":[23.4241,53.8478],
  "South Africa":[-30.5595,22.9375],"Brazil":[-14.2350,-51.9253],
  "Thailand":[15.8700,100.9925],"Iceland":[64.9631,-19.0208],"Mexico":[23.6345,-102.5528]
};
window.showTravelAnalytics=()=>{
  const history=state.trips.filter(t=>t.status==="COMPLETED");
  const countries=[...new Set(history.map(t=>t.destinationCountry))];
  const worldPercent=(countries.length/195*100).toFixed(1);
  const countryCounts=history.reduce((counts,t)=>{counts[t.destinationCountry]=(counts[t.destinationCountry]||0)+1;return counts},{});
  const continentGroups=[
    ["Asia",["Thailand","Singapore","United Arab Emirates"]],
    ["North America",["Mexico","Canada","United States"]],
    ["Europe",["Iceland","Italy","Spain"]],
    ["Africa",["South Africa"]],
    ["South America",["Brazil"]],
    ["Oceania",["Australia"]]
  ].map(([name,members])=>{
    const visited=members.filter(country=>countryCounts[country]);
    return {
      name,
      countries:visited,
      trips:visited.reduce((total,country)=>total+countryCounts[country],0),
      destinations:visited.map(country=>({
        country,
        cities:[...new Set(history.filter(trip=>trip.destinationCountry===country).map(trip=>trip.destinationCity).filter(Boolean))]
      }))
    };
  }).filter(group=>group.countries.length);
  const displayCountry=country=>country==="United Arab Emirates"?"UAE":country==="United States"?"USA":country;
  openSheet(`<button class="back" onclick="activate('home')">← Overview</button>
    <div class="analytics-title"><span class="eyebrow">YOUR GLOBAL PROFILE</span><h2>My World Story</h2></div>
    <section class="travel-story-card">
      <div id="travelWorldMap" class="travel-world-map"><span>Loading your travel map…</span></div>
      <div class="travel-metrics">
        <div><strong>${countries.length}</strong><small>COUNTRIES</small></div>
        <div><strong>${history.length}</strong><small>TRIPS</small></div>
        <div><strong>${continentGroups.length}</strong><small>CONTINENTS</small></div>
        <div><strong>${worldPercent}%</strong><small>EXPLORED</small></div>
      </div>
    </section>
    <section class="continents-explored"><div class="section-head profile-section-head"><h2>Travel Chapters</h2></div>
      <div class="continent-grid">${continentGroups.map(group=>`<details class="continent-card">
        <summary><i>✓</i><span><b>${group.name}</b><small>${group.countries.length} ${group.countries.length===1?"country":"countries"} · ${group.trips} ${group.trips===1?"trip":"trips"}</small></span><em>⌄</em></summary>
        <div class="chapter-destinations">${group.destinations.map(destination=>`<p><b>${displayCountry(destination.country)}</b><span>${destination.cities.join(" · ")}</span></p>`).join("")}</div>
      </details>`).join("")}</div>
    </section>`);
  renderTravelWorldMap(history);
};
async function renderTravelWorldMap(history){
  if(activeAnalyticsMap){activeAnalyticsMap.remove();activeAnalyticsMap=null}
  if(!window.L){$("#travelWorldMap").innerHTML="<span>Map unavailable.</span>";return}
  $("#travelWorldMap").innerHTML="";
  activeAnalyticsMap=L.map("travelWorldMap",{zoomControl:true,minZoom:1,maxZoom:10,worldCopyJump:true,attributionControl:true,
    dragging:true,touchZoom:true,scrollWheelZoom:true,doubleClickZoom:true}).setView([18,8],1);
  L.tileLayer("https://{s}.basemaps.cartocdn.com/light_nolabels/{z}/{x}/{y}{r}.png",{
    subdomains:"abcd",maxZoom:10,attribution:"© OpenStreetMap contributors © CARTO"
  }).addTo(activeAnalyticsMap);
  const counts=history.reduce((result,t)=>{result[t.destinationCountry]=(result[t.destinationCountry]||0)+1;return result},{});
  const countryStyle=count=>{
    const fill=count>=3?"#0d6b4b":count===2?"#57a87e":"#9bcf72";
    return {color:count?"#ffffff":"transparent",weight:count?1:0,fillColor:fill,fillOpacity:count?(count>=3?.92:count===2?.82:.68):0};
  };
  try{
    const response=await fetch("https://cdn.jsdelivr.net/gh/johan/world.geo.json@master/countries.geo.json");
    if(!response.ok)throw Error("Country boundaries unavailable");
    const geo=await response.json();
    L.geoJSON(geo,{style:feature=>{
      const country=normalizeMapCountry(feature.properties?.name),count=counts[country]||0;
      return countryStyle(count);
    },onEachFeature:(feature,layer)=>{
      const country=normalizeMapCountry(feature.properties?.name),count=counts[country]||0;
      if(count)layer.bindTooltip(`${country} · ${count} ${count===1?"trip":"trips"}`);
    }}).addTo(activeAnalyticsMap);
    if(counts.Singapore){
      L.polygon([[1.47,103.60],[1.48,104.03],[1.30,104.10],[1.20,103.88],[1.24,103.61]],countryStyle(counts.Singapore))
        .bindTooltip(`Singapore · ${counts.Singapore} trips`).addTo(activeAnalyticsMap);
    }
  }catch(e){$("#travelWorldMap").insertAdjacentHTML("beforeend",'<span class="map-load-error">Country shading unavailable</span>')}
}
function normalizeMapCountry(name){
  return {"United States of America":"United States","Russian Federation":"Russia"}[name]||name;
}
window.focusTravelCountry=country=>{
  const point=visitedCoordinates[country];if(!point||!activeAnalyticsMap)return;
  activeAnalyticsMap.setView(point,country==="Singapore"?9:5,{animate:true});
};

function renderTripRecap(d,trip){
  const used=Math.round(Number(d.budgetUsagePercentage||0)),over=used>100;
  const categories=Object.entries(d.categorySpending||{}).sort((a,b)=>Number(b[1])-Number(a[1]));
  const total=categories.reduce((sum,item)=>sum+Number(item[1]),0)||1;
  const colors=["#0d6b4b","#d5f16b","#e7a765","#6da7a0","#8b7cad"];let cursor=0;
  const stops=categories.map((item,index)=>{const start=cursor;cursor+=Number(item[1])/total*100;return `${colors[index%colors.length]} ${start}% ${cursor}%`}).join(",");
  const issues=(d.recentTransactions||[]).filter(t=>t.status==="DECLINED"||t.failureCode).length;
  return `<section class="trip-recap">
    <div class="recap-stamp"><i>✓</i><span><small>VOYAGE CHECK-IN</small><b>${escapeHtml(trip.destinationCity||trip.destinationCountry)}</b></span></div>
    <div class="section-head profile-section-head"><h2>Trip recap</h2><span class="${over?"over":"within"}">${over?"Over budget":"On budget"}</span></div>
    <div class="recap-visual"><div class="recap-donut" style="background:conic-gradient(${stops||"#dfe3dd 0 100%"})"><i></i></div>
      <div class="recap-facts"><b>${currencyMoney(d.spent,d.currency)} spent</b><span>${used}% of budget</span><span>${issues} payment ${issues===1?"issue":"issues"}</span></div></div>
    ${categories.length?`<div class="recap-bars">${categories.slice(0,4).map(([name,value],index)=>`<div><span>${name}</span><i><b style="width:${Math.max(8,Number(value)/total*100)}%;background:${colors[index%colors.length]}"></b></i><strong>${Math.round(Number(value)/total*100)}%</strong></div>`).join("")}</div>`:"<p>No spending recorded.</p>"}
  </section>`;
}
function renderTransactionsPage(){
  const isReported=t=>state.alerts.some(a=>a.transactionId===t.transactionId&&a.status==="REPORTED_FRAUD");
  const approved=state.transactions.filter(t=>t.status==="APPROVED"&&t.transactionType!=="REFUND"&&!isReported(t));
  const declined=state.transactions.filter(t=>t.status==="DECLINED"||isReported(t));
  const other=state.transactions.filter(t=>!approved.includes(t)&&!declined.includes(t));
  const spending={};approved.forEach(t=>spending[t.merchantCategory||"OTHER"]=(spending[t.merchantCategory||"OTHER"]||0)+Number(t.billingAmount));
  const entries=Object.entries(spending).sort((a,b)=>b[1]-a[1]),total=entries.reduce((s,e)=>s+e[1],0)||1;
  const colors=["#0d6b4b","#d5f16b","#e7a765","#6da7a0","#8b7cad","#d77a67","#9ba49e"];let cursor=0;
  const stops=entries.map((e,i)=>{const start=cursor;cursor+=e[1]/total*100;return `${colors[i%colors.length]} ${start}% ${cursor}%`}).join(",");
  const top=entries[0],observation=declined.length?`${declined.length} payment${declined.length>1?"s":""} need attention. Each has a bank-guided resolution path.`:top?`${top[0]} is your largest travel spending category at ${Math.round(top[1]/total*100)}%.`:"Your travel spending insights will appear after the first purchase.";
  openSheet(`<div class="page-title"><span class="eyebrow">YOUR TRAVEL MONEY</span><h1>Payments</h1></div>
    <div class="spending-advice"><span class="eyebrow">BANK INSIGHT</span><b>${observation}</b></div>
    <div class="transaction-summary"><div><small>TOTAL SPENT</small><b>${money(total)}</b></div><div><small>APPROVED</small><b>${approved.length}</b></div><div><small>DECLINED</small><b>${declined.length}</b></div></div>
    <div class="chart-wrap"><div class="pie-chart" style="background:conic-gradient(${stops||"#dfe3dd 0 100%"})"></div><div class="chart-legend">${entries.slice(0,7).map((e,i)=>`<div><i style="background:${colors[i%colors.length]}"></i><span>${e[0]} · ${Math.round(e[1]/total*100)}%</span></div>`).join("")}</div></div>
    <div class="transaction-group-title"><h3>Needs attention</h3><span>${declined.length} records</span></div>
    ${declined.length?declined.map(transactionRow).join(""):"<p>No payments need attention.</p>"}
    <div class="transaction-group-title"><h3>Successful payments</h3><span>${approved.length} records</span></div>
    ${approved.length?approved.map(transactionRow).join(""):"<p>No successful payments yet.</p>"}
    ${other.length?`<div class="transaction-group-title"><h3>Other activity</h3><span>${other.length} records</span></div>${other.map(transactionRow).join("")}`:""}`);
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
    <section class="section-head profile-section-head"><h2>Your cards</h2><small class="swipe-hint">Swipe →</small></section>
    <div class="card-wallet">${state.cards.map(physicalCard).join("")}</div>
    <section class="section-head profile-section-head"><h2>Test a payment</h2></section>
    <div class="payment-test-launch">
      <button class="test-lab-icon" onclick="openPaymentTest()" aria-label="Create a test payment">＋</button>
      <small>FOR DEMO PURPOSES ONLY</small>
    </div>
    <section class="section-head profile-section-head"><h2>Security</h2></section>
    <button class="bank-secondary" onclick="showAlerts()">Review fraud alerts</button><button class="bank-secondary" onclick="signOut()">Sign out securely</button>`);
}
window.openPaymentTest=()=>{
  const activeCards=state.cards.filter(c=>c.status==="ACTIVE");
  if(!activeCards.length){toast("No active card is available for testing");return}
  openSheet(`<button class="back" onclick="activate('profile')">← Profile</button>
    <span class="eyebrow">PAYMENT TEST LAB</span><h2>Record a test payment</h2>
    <p class="test-lab-intro">Create a payment to test recovery and fraud rules.</p>
    <form class="payment-test-form" id="paymentTestForm">
      <label>TEST SCENARIO<select name="scenario" onchange="applyPaymentTestScenario(this.value)">
        <option value="APPROVED">Successful travel payment</option>
        <option value="PAYMENT_INTERRUPTED">Payment interrupted</option>
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
      <label>TRANSACTION TIME<input name="transactionTime" type="datetime-local" value="2026-08-14T09:30" required><small>Change the date and time to test outside-trip, rapid-country-change and repeated-attempt rules.</small></label>
      <div class="test-pipeline"><span>Card capability</span><i>→</i><span>Live FX</span><i>→</i><span>Travel rules</span><i>→</i><span>Fraud decision</span></div>
      <button type="submit">Run payment test</button>
    </form>`);
  $("#paymentTestForm").onsubmit=submitPaymentTest;
};
window.applyPaymentTestScenario=scenario=>{
  const form=$("#paymentTestForm");if(!form)return;
  const presets={
    APPROVED:{merchantName:"Tokyo Airport Taxi",amount:"65.00",currency:"JPY",country:"Japan",city:"Tokyo",category:"TRANSPORT",transactionType:"PURCHASE"},
    PAYMENT_INTERRUPTED:{merchantName:"Tokyo Convenience Store",amount:"48.00",currency:"USD",country:"Japan",city:"Tokyo",category:"RETAIL",transactionType:"PURCHASE"},
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
  const failureCode=scenario==="PAYMENT_INTERRUPTED"?"NETWORK_ERROR":scenario;
  const payload={
    transactionId:`test-${Date.now()}-${Math.random().toString(36).slice(2,7)}`,
    cardId:data.get("cardId"),merchantName:data.get("merchantName").trim(),
    merchantCountry:data.get("country").trim(),merchantCity:data.get("city").trim(),
    merchantCategory:data.get("category"),originalAmount:Number(data.get("amount")),
    originalCurrency:data.get("currency").toUpperCase(),transactionTime:new Date(data.get("transactionTime")).toISOString(),
    transactionType:data.get("transactionType"),status:declined?"DECLINED":"APPROVED",
    failureCode:declined?failureCode:null
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
  const alerts=uniqueByTransaction(state.alerts);
  const open=alerts.filter(a=>a.status==="OPEN");
  const resolved=alerts.filter(a=>["CONFIRMED_SAFE","REPORTED_FRAUD","CARD_FROZEN","CLOSED"].includes(a.status));
  openSheet(`<button class="back" onclick="activate('home')">← Overview</button><span class="eyebrow">CARD SECURITY</span><h2>${open.length?"Check a payment":"You’re all caught up"}</h2>
    <p class="security-intro">${open.length?`${open.length} payment${open.length>1?"s":""} need${open.length===1?"s":""} your response.`:"Nothing needs your attention."}</p>
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
  try{const alert=state.alerts.find(a=>a.id===id);await api(`/api/travel/alerts/${id}/${action}`,{method:"POST"});await load();toast(action==="report"?"Fraud reported and a case was opened":"Payment confirmed as yours");if(action==="confirm"&&alert?.transactionId)paymentHelp(alert.transactionId);else showAlerts()}catch(e){toast(e.message)}
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
$("#close").onclick=()=>activate("home");
$("#refresh").onclick=async()=>{await load();toast("Journeys refreshed")};
$("#createTrip").onclick=openCreateTrip;
$("#headerAnalytics").onclick=showTravelAnalytics;
$("#headerAtms").onclick=()=>{
  const trip=state.trips.find(t=>t.status!=="COMPLETED"&&t.status!=="CANCELLED")||state.trips[0];
  if(!trip){toast("Create a trip to search near your destination");return}
  planCashExchange(trip.id,"overview");
};

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
  const name=$("#authCustomer").value.trim();
  if(!name){toast("Enter your name");return}
  try{preferredAuth(await api("/api/auth/start",{method:"POST",body:JSON.stringify({customerId:"customer-001"})}))}
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
window.restartAuth=()=>{$("#authBody").innerHTML=`<h2>Welcome back</h2><p>Verify your identity to open Travel Assistant.</p><label class="auth-label">YOUR NAME<input id="authCustomer" value="Jessie Han" autocomplete="name"></label><button class="bank-primary" id="authStart">Continue securely</button>`;$("#authStart").onclick=startAuth};
window.signOut=async()=>{try{if(authToken)await api("/api/auth/logout",{method:"POST"})}catch(e){}showAuth();restartAuth()};
$("#authStart").onclick=startAuth;
updateGreeting();
setInterval(updateGreeting,60000);
(async()=>{if(!authToken){showAuth();return}try{await api("/api/auth/session");$("#authGate").classList.add("hidden");load()}catch(e){showAuth()}})();
