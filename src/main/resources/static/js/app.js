
let activeMode = null; // 'SERIAL' or 'BLUETOOTH'
let globalSerialWriter = null;
let globalBtCharacteristic = null;

// Mobile Device Detector
const isMobileDevice = /Android|iPhone|iPad|iPod/i.test(navigator.userAgent);

function notify(msg, type = 'info') {
    let bg = type === 'success' ? '#198754' : type === 'error' ? '#dc3545' : '#0dcaf0';
    if (typeof Toastify !== 'undefined') {
        Toastify({ text: msg, duration: 3000, style: { background: bg, borderRadius: '6px' } }).showToast();
    } else {
        alert(msg);
    }
}

document.addEventListener("DOMContentLoaded", () => {
    fetchRecentScans();
});

// Process Scan and Auto-Print if enabled
async function processLpnScan() {
    const input = document.getElementById("lpnInput");
    if (!input || !input.value.trim()) return;

    const lpn = input.value.trim();

    try {
        const response = await fetch(`/api/lpn/scan?lpn=${encodeURIComponent(lpn)}`);
        const data = await response.json();

        if (response.ok) {
            notify(`LPN Found: ${data.lpn}`, "success");
            renderScanResult(data);
            fetchRecentScans();

            // Auto-Print Trigger
            const isAutoPrint = document.getElementById("autoPrintCheck")?.checked;
            if (isAutoPrint) {
                printTsplLabel(data);
            }
        } else {
            notify(data.message || "LPN NOT FOUND", "error");
        }
    } catch (e) {
        notify("Backend Connection Failure", "error");
    } finally {
        input.value = "";
        hideKeyboardAndRefocus(input);
    }
}

function hideKeyboardAndRefocus(input) {
    input.blur();

    setTimeout(() => {
        input.setAttribute("readonly", "readonly");
        input.focus();

        setTimeout(() => input.removeAttribute("readonly"), 50);
    }, 150);
}

// Render 16 Zones (8 Pairs Layout) inside Screen UI
function renderScanResult(data) {
    const card = document.getElementById("resultCard");
    if (!card) return;
    card.style.display = "block";

    const pairs = data.ptlMergedGroups ? data.ptlMergedGroups.split(",") : [];

    let zonesHtml = '<div class="row g-2 mt-2">';
    pairs.forEach(p => {
        zonesHtml += `<div class="col-6 col-md-3"><div class="zone-pair-badge">${p.trim()}</div></div>`;
    });
    zonesHtml += '</div>';

    card.innerHTML = `
        <div class="d-flex justify-content-between align-items-center mb-2 border-bottom pb-2">
            <h6 class="fw-bold m-0 text-success"><i class="bi bi-check-circle-fill me-1"></i> LPN MATCHED</h6>
            <button class="btn btn-sm btn-vm-primary" onclick='printTsplLabel(${JSON.stringify(data)})'><i class="bi bi-printer me-1"></i> Print Label</button>
        </div>
        <div class="row g-2">
            <div class="col-4"><strong>LPN:</strong> ${data.lpn}</div>
            <div class="col-4"><strong>Group:</strong> ${data.ptlGroup}</div>
            <div class="col-4"><strong>Location:</strong> ${data.location}</div>
        </div>
        <div class="mt-2"><small class="fw-bold text-muted">ZONES</small>${zonesHtml}</div>
    `;
}

// Fetch Top 10 Recent Scans
async function fetchRecentScans() {
    try {
        const res = await fetch("/api/lpn/recent-scans");
        const list = await res.json();
        const tbody = document.getElementById("recentScansBody");
        if (!tbody) return;

        if (!list || list.length === 0) {
            tbody.innerHTML = `<tr><td colspan="5" class="text-center py-3 text-muted">No records</td></tr>`;
            return;
        }

        tbody.innerHTML = list.slice(0, 10).map(s => `
            <tr>
                <td class="fw-bold">${s.lpn}</td>
                <td><span class="badge bg-secondary">${s.ptlGroup}</span></td>
                <td>${s.location}</td>
                <td><small class="fw-semibold text-truncate d-inline-block" style="max-width: 300px;">${s.ptlMergedGroups}</small></td>
                <td><small class="text-muted">${new Date(s.scannedAt).toLocaleTimeString()}</small></td>
            </tr>
        `).join("");
    } catch (e) {
        console.error("Scan fetch error:", e);
    }
}

/**
 * EXACT CONNECTION LOGIC FROM DIRECT TEXT PRINTER
 * Auto-detects Mobile vs Laptop and handles GATT/Serial Connection
 */
async function connectPrinter() {
    const statusText = document.getElementById("printerStatusText");

    if (isMobileDevice) {
        // MOBILE / TABLET (Web Bluetooth)
        if (!navigator.bluetooth) {
            notify("Bluetooth API unavailable! Chrome flags check karein.", "error");
            return;
        }

        try {
            if (statusText) statusText.innerText = "Scanning Bluetooth...";

            const device = await navigator.bluetooth.requestDevice({
                acceptAllDevices: true,
                optionalServices: [
                    '000018f0-0000-1000-8000-00805f9b34fb',
                    '00001101-0000-1000-8000-00805f9b34fb'
                ]
            });

            if (statusText) statusText.innerText = "Connecting to " + device.name + "...";
            const server = await device.gatt.connect();
            const services = await server.getPrimaryServices();

            globalBtCharacteristic = null;
            for (let service of services) {
                const characteristics = await service.getCharacteristics();
                for (let c of characteristics) {
                    if (c.properties.write || c.properties.writeWithoutResponse) {
                        globalBtCharacteristic = c;
                        break;
                    }
                }
                if (globalBtCharacteristic) break;
            }

            if (globalBtCharacteristic) {
                activeMode = 'BLUETOOTH';
                if (statusText) {
                    statusText.innerText = "CONNECTED (BT: " + device.name + ")";
                    statusText.className = "m-0 fw-bold text-success";
                }
                notify("Bluetooth Printer Connected!", "success");
            } else {
                notify("Bluetooth device found, but write channel missing.", "error");
            }
        } catch (err) {
            console.error(err);
            if (statusText) {
                statusText.innerText = "Connection Failed";
                statusText.className = "m-0 fw-bold text-danger";
            }
            notify("Mobile Bluetooth Connection Failed", "error");
        }

    } else {
        // LAPTOP / PC (Web Serial)
        if (!("serial" in navigator)) {
            notify("Browser does not support Web Serial API", "error");
            return;
        }

        try {
            if (statusText) statusText.innerText = "Selecting Port...";
            const port = await navigator.serial.requestPort();
            await port.open({ baudRate: 9600 });
            globalSerialWriter = port.writable.getWriter();

            activeMode = 'SERIAL';
            if (statusText) {
                statusText.innerText = "CONNECTED (SERIAL)";
                statusText.className = "m-0 fw-bold text-success";
            }
            notify("Thermal Printer Ready!", "success");
        } catch (err) {
            console.error(err);
            if (statusText) {
                statusText.innerText = "Connection Failed";
                statusText.className = "m-0 fw-bold text-danger";
            }
            notify("Printer Connection Cancelled", "error");
        }
    }
}

// TSPL Printing Engine with 16 Zones Formatting (Fixed for Location & Zones without 'Z')
async function printTsplLabel(data) {
    if (!activeMode) {
        notify("Printer Not Connected! Click 'Connect Printer' first.", "error");
        return;
    }

    // Safely extract string values to prevent undefined errors
    const lpn = String(data.lpn || '').trim();
    const group = String(data.ptlGroup || '').trim();
    const location = String(data.location || '').trim();

    let zones = [];
    if (data.ptlMergedGroups) {
        zones = Array.isArray(data.ptlMergedGroups)
            ? data.ptlMergedGroups
            : String(data.ptlMergedGroups).split(',').map(s => s.trim()).filter(Boolean);
    }

    // Sort Zones Ascending Order (Handles Numbers as well as Strings)
    zones.sort((a, b) => String(a).localeCompare(String(b), undefined, { numeric: true, sensitivity: 'base' }));

    // 4x4 Grid Zones - Adjusted spacing to prevent overlapping
    const boxWidth = 110;
    const boxHeight = 26;
    const boxGapX = 10;
    const startX = 35;

// CHANGE 1: Grid thoda upar se start karein taaki neeche space mile
    const startY = 155; // Pehle 185 tha

// CHANGE 2: Row Gap ko 34 se badha kar 42 karein
    const rowGapY = 45;

    let zonesTspl = "";

    for (let i = 0; i < Math.min(zones.length, 16); i++) {
        const row = Math.floor(i / 4);
        const col = i % 4;

        const x = startX + col * (boxWidth + boxGapX);
        const y = startY + row * rowGapY;

        const textX = x + 20;
        const textY = y;

        // Font 3 aur Y-scale 2 (Bada height aur bilkul clear gap)
        zonesTspl += `TEXT ${textX},${textY},"3",0,2,2,"${zones[i]}"\n`;
    }


    // 💡 RELIABLE MOBILE & PC DATE-TIME FORMATTING
    const now = new Date();

// Date Breakdown (DD/MM/YYYY)
    const day = now.getDate().toString().padStart(2, '0');
    const month = (now.getMonth() + 1).toString().padStart(2, '0');
    const year = now.getFullYear();

// 🛠 FIX: getHours() ki jagah Intl.DateTimeFormat use kiya, hour12:false FORCE karke,
    const dateTimeStr = await getReliableDateTimeStr();



    // EXACT TSPL COMMAND STRING WITH LOCATION
    const tspl = `SIZE 70 mm, 48 mm
    GAP 2 mm, 0
    DIRECTION 0
    CLS
    
    BOX 20,10,540,370,3
    
    TEXT 280,18,"3",0,1,1,2,"V-MART PALWAL"
    BAR 20,45,520,2
    
    TEXT 35,62,"3",0,1,1,"LPN:"
    BARCODE 120,52,"128",32,1,0,2,2,"${lpn}"
    
    BAR 20,110,520,2
    
    TEXT 35,120,"3",0,1,1,"GRP: ${group}"
    TEXT 320,120,"3",0,1,1,"LOC: ${location}"
    BAR 20,150,520,2
    
    ${zonesTspl}
    
    // 💡 10 Points Shifted Down
    BAR 20,342,520,2
    TEXT 35,352,"2",0,1,1,"Printed On:  ${dateTimeStr}"
    
    PRINT 1,1
    `;

    const bytes = new TextEncoder().encode(tspl);

    try {
        if (activeMode === 'SERIAL' && globalSerialWriter) {
            await globalSerialWriter.write(bytes);
            notify("Label Sent to Printer", "success");

        } else if (activeMode === 'BLUETOOTH' && globalBtCharacteristic) {

            // BLE Chunking Logic
            const CHUNK_SIZE = 100;
            for (let offset = 0; offset < bytes.length; offset += CHUNK_SIZE) {
                const chunk = bytes.slice(offset, offset + CHUNK_SIZE);
                await globalBtCharacteristic.writeValue(chunk);
                await new Promise(resolve => setTimeout(resolve, 20));
            }

            notify("Label Sent to Bluetooth Printer", "success");

        } else {
            notify("Printer connection channel lost. Reconnect printer.", "error");
        }
    } catch (e) {
        notify("Printing Failed: " + e.message, "error");
    }
}

// Handle Modal CSV Upload Trigger
async function uploadCsvFromModal() {
    const fileInput = document.getElementById("modalCsvInput");
    if (!fileInput.files[0]) {
        notify("Select a CSV file first", "error");
        return;
    }

    const formData = new FormData();
    formData.append("file", fileInput.files[0]);

    try {
        const res = await fetch("/api/lpn/upload-csv", { method: "POST", body: formData });
        const data = await res.json();
        if (res.ok) {
            notify(`Success! ${data.totalRows} Rows Uploaded.`, "success");
            fetchRecentScans();
            bootstrap.Modal.getInstance(document.getElementById('csvUploadModal')).hide();
        } else {
            notify(data.message, "error");
        }
    } catch (e) {
        notify("CSV Upload Exception", "error");
    }
}

// 💡 ONLINE TIME FETCH (internet se sahi time + AM/PM), fallback = device time
async function getReliableDateTimeStr() {

    try {

        const controller = new AbortController();

        const timeoutId = setTimeout(() => controller.abort(), 3000);

        const res = await fetch(
            "https://worldtimeapi.org/api/timezone/Asia/Kolkata",
            {
                signal: controller.signal
            }
        );

        clearTimeout(timeoutId);

        if (!res.ok) throw new Error("Time API failed");

        const data = await res.json();

        console.log("WORLD API RESPONSE =", data);

        const dt = new Date(data.datetime);

        console.log("API DATE =", dt);
        console.log("API HOURS =", dt.getHours());

        return formatDateTime(dt);

    } catch (e) {

        console.log("WORLD API FAILED");
        console.log(e);

        const local = new Date();

        console.log("LOCAL DATE =", local);
        console.log("LOCAL HOURS =", local.getHours());

        return formatDateTime(local);
    }
}

// Common formatter — chahe online time ho ya device time, isi se format hoga
function formatDateTime(now) {

    const day = String(now.getDate()).padStart(2, '0');
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const year = now.getFullYear();

    const hour24 = now.getHours();
    const minutes = String(now.getMinutes()).padStart(2, '0');

    const ampm = hour24 >= 12 ? "PM" : "AM";

    let hour12 = hour24 % 12;
    if (hour12 === 0) hour12 = 12;

    const formattedHours = String(hour12).padStart(2, '0');

    const result = `${day}/${month}/${year} ${formattedHours}:${minutes} ${ampm}`;
    return result;
}
