# **Travel Assistant Project Schedule**

## **Phase 1: Before Trip \- Trip Setup and Travel Readiness**

### **Goal**

Allow users to create a trip and immediately check whether their cards and accounts are ready for international travel.

### **Key Features**

* Add destination, travel dates, budget, currency and preferred card  
* Check card expiry, overseas payment status, limits and balance  
* Generate a readiness score  
* Show warnings and recommended actions

### **To-dos**

* Build the Trip model and APIs  
* Design the combined trip setup page  
* Create readiness rules  
* Add card and account checks  
* Display score, warnings and quick actions  
* Test invalid dates, expired cards and disabled overseas payments

---

## **Phase 2: During Trip \- Payment Tracking and Failure Support**

### **Goal**

Record successful payments and explain failed payments with clear solutions.

### **Key Features**

* Add successful payments to travel expenses  
* Track spending and remaining budget  
* Show merchant, currency, exchange rate and category  
* Explain why a payment failed  
* Recommend actions such as increasing limits or enabling overseas payments

### **To-dos**

* Build transaction APIs  
* Link transactions to trips  
* Create the Travel Dashboard  
* Add spending categories and budget calculations  
* Map failure codes to customer-friendly messages  
* Add card control actions  
* Test success, insufficient balance, frozen card and limit failures

---

## **Phase 3: Ongoing Monitor \- Suspicious Transaction Monitoring**

### **Goal**

Detect unusual overseas transactions and allow users to respond quickly.

### **Key Features**

* Detect transactions outside trip dates or destinations  
* Identify duplicate charges and unusual withdrawals  
* Show the reason and risk level  
* Allow users to confirm, report or freeze the card

### **To-dos**

* Define suspicious transaction rules  
* Build the monitoring rule engine  
* Create alerts automatically  
* Design alert and fraud report pages  
* Connect alerts to card freezing  
* Test location, duplicate and high-value transaction scenarios

---

## **Final Flow**

Create Trip → Readiness Check → Payment Attempt → Record Success or Explain Failure → Monitor Transactions → Confirm, Report or Freeze