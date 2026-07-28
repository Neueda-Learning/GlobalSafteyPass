document.addEventListener('DOMContentLoaded', function () {
    const path = window.location.pathname;
    document.querySelectorAll('.phone-nav a[data-nav]').forEach(function (link) {
        const nav = link.getAttribute('data-nav');
        if (nav === '/' ? path === '/' : path.startsWith(nav)) {
            link.classList.add('active');
        }
    });

    const form = document.getElementById('paymentForm');
    if (!form) return;

    form.addEventListener('submit', async function (e) {
        e.preventDefault();
        const resultDiv = document.getElementById('paymentResult');
        resultDiv.innerHTML = '<div class="text-center"><div class="spinner-border spinner-border-sm"></div> Processing...</div>';

        const payload = {
            tripId: parseInt(document.getElementById('tripId').value),
            cardId: parseInt(document.getElementById('cardId').value),
            merchant: document.getElementById('merchant').value,
            amount: parseFloat(document.getElementById('amount').value),
            currency: document.getElementById('currency').value,
            exchangeRate: parseFloat(document.getElementById('exchangeRate').value),
            category: document.getElementById('category').value,
            location: document.getElementById('location').value,
            simulateFailure: document.getElementById('simulateFailure').value || null
        };

        try {
            const resp = await fetch('/api/payments', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'same-origin',
                body: JSON.stringify(payload)
            });
            const data = await resp.json();

            if (data.status === 'SUCCESS') {
                resultDiv.innerHTML = '<div class="alert alert-success"><i class="bi bi-check-circle"></i> Payment successful! Refreshing...</div>';
                setTimeout(() => location.reload(), 1500);
            } else {
                resultDiv.innerHTML = '<div class="alert alert-danger"><strong>' + data.failureCode + '</strong><br>' + data.failureMessage + '</div>';
                setTimeout(() => location.reload(), 3000);
            }
        } catch (err) {
            resultDiv.innerHTML = '<div class="alert alert-danger">Request failed: ' + err.message + '</div>';
        }
    });
});
