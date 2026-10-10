let selectedFromDate = null;
let selectedToDate = null;
let lastPreviewCount = 0;

function notify(msg, type = 'info') {
    let bg = type === 'success' ? '#198754' : type === 'error' ? '#dc3545' : '#0dcaf0';
    if (typeof Toastify !== 'undefined') {
        Toastify({ text: msg, duration: 3500, style: { background: bg, borderRadius: '6px' } }).showToast();
    } else {
        alert(msg);
    }
}

document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll('.range-preset-btn').forEach(btn => {
        btn.addEventListener('click', () => selectPreset(btn));
    });

    document.getElementById('confirmCheckbox').addEventListener('change', updateFinalDeleteButtonState);
    document.getElementById('deletePasswordInput').addEventListener('input', updateFinalDeleteButtonState);
});

function selectPreset(btn) {
    document.querySelectorAll('.range-preset-btn').forEach(b => b.classList.remove('btn-vm-primary', 'active'));
    btn.classList.add('btn-vm-primary', 'active');

    // Let the backend resolve the exact boundary for the named range;
    // here we just reflect a readable range in the date inputs for the user.
    const now = new Date();
    let from = new Date();

    switch (btn.dataset.range) {
        case 'today':
            from.setHours(0, 0, 0, 0);
            break;
        case 'week': {
            const day = now.getDay() === 0 ? 7 : now.getDay(); // Monday = 1
            from = new Date(now);
            from.setDate(now.getDate() - (day - 1));
            from.setHours(0, 0, 0, 0);
            break;
        }
        case 'month':
            from = new Date(now.getFullYear(), now.getMonth(), 1);
            break;
        case 'qtd': {
            const qStartMonth = Math.floor(now.getMonth() / 3) * 3;
            from = new Date(now.getFullYear(), qStartMonth, 1);
            break;
        }
        case 'year':
            from = new Date(now.getFullYear(), 0, 1);
            break;
    }

    document.getElementById('deleteFromDate').value = toDateTimeLocal(from);
    document.getElementById('deleteToDate').value = toDateTimeLocal(now);
}

function toDateTimeLocal(date) {
    const pad = n => String(n).padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

async function previewDelete() {
    const fromDate = document.getElementById('deleteFromDate').value;
    const toDate = document.getElementById('deleteToDate').value;

    if (!fromDate || !toDate) {
        notify('Please select both From and To dates', 'error');
        return;
    }

    selectedFromDate = fromDate;
    selectedToDate = toDate;

    try {
        const params = new URLSearchParams({
            fromDate: fromDate + ':00',
            toDate: toDate + ':00'
        });
        const res = await fetch(`/api/v1/master-data/delete-preview?${params.toString()}`);
        const data = await res.json();

        if (!res.ok) {
            notify(data.message || 'Preview failed', 'error');
            return;
        }

        lastPreviewCount = data.matchingRecords;
        document.getElementById('previewCount').innerText = lastPreviewCount;
        document.getElementById('previewResultCard').style.display = 'block';
        document.getElementById('deleteConfirmBtn').disabled = lastPreviewCount === 0;

    } catch (e) {
        notify('Backend Connection Failure', 'error');
        console.error(e);
    }
}

function openPasswordPrompt() {
    document.getElementById('passwordModalCount').innerText = lastPreviewCount;
    document.getElementById('deletePasswordInput').value = '';
    document.getElementById('confirmCheckbox').checked = false;
    updateFinalDeleteButtonState();

    const modal = new bootstrap.Modal(document.getElementById('deletePasswordModal'));
    modal.show();
}

function updateFinalDeleteButtonState() {
    const password = document.getElementById('deletePasswordInput').value;
    const confirmed = document.getElementById('confirmCheckbox').checked;
    document.getElementById('finalDeleteBtn').disabled = !(password.length > 0 && confirmed);
}

async function confirmDelete() {
    const password = document.getElementById('deletePasswordInput').value;

    try {
        const res = await fetch('/api/v1/master-data/delete', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                fromDate: selectedFromDate + ':00',
                toDate: selectedToDate + ':00',
                password: password
            })
        });

        const data = await res.json();

        if (!res.ok) {
            notify(data.message || 'Delete failed — check password', 'error');
            return;
        }

        notify(data.message || `${data.deletedCount} records deleted`, 'success');

        const modalEl = document.getElementById('deletePasswordModal');
        bootstrap.Modal.getInstance(modalEl)?.hide();

        document.getElementById('previewResultCard').style.display = 'none';
        document.getElementById('deleteFromDate').value = '';
        document.getElementById('deleteToDate').value = '';
        document.querySelectorAll('.range-preset-btn').forEach(b => b.classList.remove('btn-vm-primary', 'active'));

    } catch (e) {
        notify('Backend Connection Failure', 'error');
        console.error(e);
    }
}
