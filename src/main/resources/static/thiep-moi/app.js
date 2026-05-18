const state = {
    images: [
        "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 400 300'%3E%3Crect width='400' height='300' fill='%23f2d8c8'/%3E%3Ccircle cx='92' cy='74' r='56' fill='%23d94d46'/%3E%3Ccircle cx='315' cy='72' r='52' fill='%239a263d'/%3E%3Crect x='42' y='130' width='316' height='116' rx='18' fill='%23315f5a'/%3E%3Cpath d='M90 230c36-72 76-72 112 0' fill='%23fff7eb'/%3E%3Cpath d='M206 232c24-58 64-66 104 0' fill='%23fdf5e8'/%3E%3Ccircle cx='144' cy='120' r='28' fill='%23f6d1b8'/%3E%3Ccircle cx='250' cy='119' r='28' fill='%23f6d1b8'/%3E%3Ctext x='200' y='281' text-anchor='middle' font-family='Arial' font-size='20' fill='%23fff7eb'%3EẢnh cưới%3C/text%3E%3C/svg%3E",
        "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 400 300'%3E%3Crect width='400' height='300' fill='%23e8eef4'/%3E%3Crect x='32' y='52' width='336' height='190' rx='16' fill='%23293d59'/%3E%3Cpath d='M70 226c40-82 84-82 124 0' fill='%23fff9f0'/%3E%3Cpath d='M196 226c34-76 82-76 128 0' fill='%23f3eadb'/%3E%3Ccircle cx='138' cy='112' r='30' fill='%23f2c7a8'/%3E%3Ccircle cx='258' cy='112' r='30' fill='%23f2c7a8'/%3E%3Cpath d='M70 62h260' stroke='%23d6b56d' stroke-width='10'/%3E%3Ctext x='200' y='280' text-anchor='middle' font-family='Arial' font-size='20' fill='%23293d59'%3EKhoảnh khắc%3C/text%3E%3C/svg%3E",
        "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 400 300'%3E%3Crect width='400' height='300' fill='%23f8f2e7'/%3E%3Cpath d='M0 220h400v80H0z' fill='%23c2b39d'/%3E%3Ccircle cx='200' cy='115' r='74' fill='%23d6b56d'/%3E%3Ctext x='200' y='126' text-anchor='middle' font-family='Arial' font-size='42' fill='%23fffaf0'%3E囍%3C/text%3E%3Ctext x='200' y='270' text-anchor='middle' font-family='Arial' font-size='20' fill='%23315f5a'%3EAlbum%3C/text%3E%3C/svg%3E"
    ],
    activeImageIndex: 0
};

const imageInput = document.getElementById("imageInput");

function byId(id) {
    return document.getElementById(id);
}

function setText(id, value) {
    byId(id).textContent = value;
}

function formatDateParts(value) {
    const date = value ? new Date(`${value}T00:00:00`) : new Date();
    const weekdays = ["Chủ nhật", "Thứ hai", "Thứ ba", "Thứ tư", "Thứ năm", "Thứ sáu", "Thứ bảy"];
    return {
        weekday: weekdays[date.getDay()],
        day: String(date.getDate()).padStart(2, "0"),
        month: `Tháng ${date.getMonth() + 1}`,
        year: String(date.getFullYear())
    };
}

function syncPreview() {
    setText("groomNamePreview", byId("groomNameInput").value || "Chú rể");
    setText("brideNamePreview", byId("brideNameInput").value || "Cô dâu");
    setText("timePreview", byId("weddingTimeInput").value || "--:--");
    setText("venuePreview", byId("venueInput").value || "Địa điểm tổ chức");
    setText("messagePreview", byId("messageInput").value || "");

    const parts = formatDateParts(byId("weddingDateInput").value);
    setText("weekdayPreview", parts.weekday);
    setText("dayPreview", parts.day);
    setText("monthPreview", parts.month);
    setText("yearPreview", parts.year);
}

function renderImages() {
    state.images.forEach((src, index) => {
        const preview = byId(`previewImage${index}`);
        const thumb = byId(`thumbImage${index}`);
        if (preview) {
            preview.src = src;
        }
        if (thumb) {
            thumb.src = src;
        }
    });
    document.querySelectorAll(".thumb").forEach((thumb) => {
        thumb.classList.toggle("active", Number(thumb.dataset.imageIndex) === state.activeImageIndex);
    });
    setText("photoCount", `${state.images.length} / 10 ảnh`);
}

function openImagePicker(index) {
    state.activeImageIndex = Number(index) || 0;
    renderImages();
    imageInput.click();
}

function setAccent(color) {
    document.documentElement.style.setProperty("--accent", color);
    document.documentElement.style.setProperty("--accent-soft", `${color}24`);
    document.querySelectorAll(".color-dot").forEach((button) => {
        button.classList.toggle("active", button.dataset.color === color);
    });
}

function setActivePanel(panelName) {
    document.querySelectorAll("[data-panel]").forEach((button) => {
        button.classList.toggle("active", button.dataset.panel === panelName);
    });
}

document.querySelectorAll("input, textarea").forEach((input) => {
    input.addEventListener("input", syncPreview);
});

byId("fontSelect").addEventListener("change", (event) => {
    document.querySelectorAll(".name-block h1").forEach((name) => {
        name.style.fontFamily = event.target.value;
    });
});

document.querySelectorAll(".image-picker").forEach((button) => {
    button.addEventListener("click", () => openImagePicker(button.dataset.imageIndex));
});

imageInput.addEventListener("change", (event) => {
    const file = event.target.files && event.target.files[0];
    if (!file) {
        return;
    }
    const url = URL.createObjectURL(file);
    state.images[state.activeImageIndex] = url;
    renderImages();
    imageInput.value = "";
});

document.querySelectorAll(".color-dot").forEach((button) => {
    button.addEventListener("click", () => setAccent(button.dataset.color));
});

document.querySelectorAll("[data-panel]").forEach((button) => {
    button.addEventListener("click", () => setActivePanel(button.dataset.panel));
});

syncPreview();
renderImages();
