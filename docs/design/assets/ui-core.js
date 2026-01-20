/**
 * BoBB UI Core Script v1.0
 * Aligned with STD-07 (I18n First) & Resource-based Localization
 */

const UI_CONFIG = {
    currentLang: localStorage.getItem('bobbuy_lang') || 'zh',
    supportedLangs: ['zh', 'zt', 'en', 'ja'],
    i18nPath: '../assets/i18n.json'
};

let i18nDictionary = {};

/**
 * Load external translations
 */
async function loadTranslations() {
    // If I18N_DATA is already defined (via script include), use it directly
    if (typeof I18N_DATA !== 'undefined') {
        i18nDictionary = I18N_DATA;
        updateTranslations();
        updateLangButtons();
        console.log('Using local I18N_DATA (CORS bypass)');
        return;
    }

    try {
        const response = await fetch(UI_CONFIG.i18nPath);
        const data = await response.json();
        i18nDictionary = data;
        updateTranslations();
        updateLangButtons();
    } catch (error) {
        console.error('Failed to load i18n resources:', error);
    }
}

function updateTranslations() {
    const elements = document.querySelectorAll('[data-i18n]');
    elements.forEach(el => {
        const key = el.getAttribute('data-i18n');
        if (i18nDictionary[key] && i18nDictionary[key][UI_CONFIG.currentLang]) {
            el.textContent = i18nDictionary[key][UI_CONFIG.currentLang];
        }
    });

    // Support for placeholders
    const inputs = document.querySelectorAll('[data-i18n-placeholder]');
    inputs.forEach(el => {
        const key = el.getAttribute('data-i18n-placeholder');
        if (i18nDictionary[key] && i18nDictionary[key][UI_CONFIG.currentLang]) {
            el.placeholder = i18nDictionary[key][UI_CONFIG.currentLang];
        }
    });
}

function switchLanguage(lang) {
    if (UI_CONFIG.supportedLangs.includes(lang)) {
        UI_CONFIG.currentLang = lang;
        localStorage.setItem('bobbuy_lang', lang);
        updateTranslations();
        updateLangButtons();
        // Update document lang attribute
        document.documentElement.lang = lang === 'zt' ? 'zh-Hant' : (lang === 'zh' ? 'zh-CN' : lang);
    }
}

function updateLangButtons() {
    const btns = document.querySelectorAll('.lang-btn');
    btns.forEach(btn => {
        const onclickAttr = btn.getAttribute('onclick');
        if (onclickAttr) {
            const match = onclickAttr.match(/'(.*?)'/);
            if (match && match[1] === UI_CONFIG.currentLang) {
                btn.classList.add('active');
            } else {
                btn.classList.remove('active');
            }
        }
    });
}

// Global UI Enhancements
async function initUI() {
    // 1. Add noise texture overlay
    const noise = document.createElement('div');
    noise.className = 'noise-overlay';
    document.body.appendChild(noise);

    // 2. Load and Apply Translations
    await loadTranslations();

    console.log('BoBB UI Core v1.1 Initialized (Resource-based I18n)');
}

document.addEventListener('DOMContentLoaded', initUI);
