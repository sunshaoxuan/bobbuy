/**
 * BoBB Merchant Framework v1.0
 * Modularized Sidebar, Header, and Footer injection
 */

const MERCHANT_FRAMEWORK = {
    isCollapsed: localStorage.getItem('merchant_sidebar_collapsed') === 'true',

    // Define Sidebar, Header, and Footer templates
    templates: {
        sidebar: `
            <aside id="main-sidebar" class="bg-white border-r border-surface flex flex-col z-50 transition-sidebar h-screen ${localStorage.getItem('merchant_sidebar_collapsed') === 'true' ? 'sidebar-mini' : 'w-64'}">
                <!-- Logo & Brand -->
                <div class="h-20 flex items-center gap-3 px-6 border-b border-surface shrink-0">
                    <div class="logo-container w-9 h-9 bg-white rounded-xl shadow-lg flex items-center justify-center rotate-3 border border-surface overflow-hidden shrink-0">
                        <img src="../assets/logo.svg" alt="BoBB" class="w-6 h-6">
                    </div>
                    <span class="brand-text text-xl font-black text-primary tracking-tight transition-all duration-300" data-i18n="APP_BRAND_NAME">BoBB</span>
                </div>

                <!-- Navigation items -->
                <nav class="flex-1 overflow-y-auto p-4 space-y-2">
                    <a href="UID0100_merchant_main.html" data-page="UID0100" class="sidebar-item flex items-center gap-3 px-4 py-3 rounded-xl font-bold text-sm">
                        <span class="material-symbols-outlined text-xl shrink-0">dashboard</span>
                        <span class="sidebar-text transition-all duration-300" data-i18n="TITLE_DASHBOARD">仪表盘</span>
                    </a>
                    <a href="UID0110_merchants_project_detail.html" data-page="UID0110" class="sidebar-item flex items-center gap-3 px-4 py-3 rounded-xl font-bold text-sm">
                        <span class="material-symbols-outlined text-xl shrink-0">explore</span>
                        <span class="sidebar-text transition-all duration-300" data-i18n="TITLE_TRIP_MGMT">行程管理</span>
                    </a>
                    <a href="UID0120_merchants_product_mgmt.html" data-page="UID0120" class="sidebar-item flex items-center gap-3 px-4 py-3 rounded-xl font-bold text-sm">
                        <span class="material-symbols-outlined text-xl shrink-0">inventory_2</span>
                        <span class="sidebar-text transition-all duration-300" data-i18n="TITLE_PRODUCT_MGMT">商品维护</span>
                    </a>
                    <a href="#" data-page="UID0130" class="sidebar-item flex items-center gap-3 px-4 py-3 rounded-xl font-bold text-sm">
                        <span class="material-symbols-outlined text-xl shrink-0">list_alt</span>
                        <span class="sidebar-text transition-all duration-300" data-i18n="TITLE_ORDER_LIST">订单列表</span>
                    </a>
                    <a href="UID0115_merchants_project_talk.html" data-page="UID0115" class="sidebar-item flex items-center justify-between px-4 py-3 rounded-xl font-bold text-sm group">
                        <div class="flex items-center gap-3">
                            <span class="material-symbols-outlined text-xl shrink-0">forum</span>
                            <span class="sidebar-text transition-all duration-300" data-i18n="MSG_CHAT_CENTER">消息中心</span>
                        </div>
                        <span class="badge w-5 h-5 bg-primary text-[10px] text-white rounded-full flex items-center justify-center opacity-0 group-[.active]:opacity-100 transition-opacity">12</span>
                    </a>
                    <a href="UID0114_merchants_project_reciept.html" data-page="UID0114" class="sidebar-item flex items-center gap-3 px-4 py-3 rounded-xl font-bold text-sm">
                        <span class="material-symbols-outlined text-xl shrink-0">local_shipping</span>
                        <span class="sidebar-text transition-all duration-300" data-i18n="TITLE_LOGISTICS">配送中心</span>
                    </a>
                    <a href="UID0113_merchants_project_settlement.html" data-page="UID0113" class="sidebar-item flex items-center gap-3 px-4 py-3 rounded-xl font-bold text-sm">
                        <span class="material-symbols-outlined text-xl shrink-0">account_balance_wallet</span>
                        <span class="sidebar-text transition-all duration-300" data-i18n="TITLE_FINANCE">财务中心</span>
                    </a>
                    <div class="pt-4 mt-4 border-t border-surface opacity-50"></div>
                    <a href="#" data-page="HELP" class="sidebar-item flex items-center gap-3 px-4 py-3 rounded-xl font-bold text-sm">
                        <span class="material-symbols-outlined text-xl shrink-0">help</span>
                        <span class="sidebar-text transition-all duration-300" data-i18n="TITLE_HELP">帮助中心</span>
                    </a>
                </nav>
                
                <!-- Collapse Toggle (Bottom) -->
                <div class="p-4 border-t border-surface">
                    <button id="sidebar-toggle" class="w-full flex items-center gap-3 px-4 py-3 rounded-xl text-text-sub hover:bg-surface hover:text-primary transition-all font-bold text-sm">
                        <span class="material-symbols-outlined text-xl shrink-0 transition-transform duration-300 ${localStorage.getItem('merchant_sidebar_collapsed') === 'true' ? 'rotate-180' : ''}">menu_open</span>
                        <span class="sidebar-text transition-all duration-300">收起导航</span>
                    </button>
                </div>
            </aside>
        `,
        header: (title) => `
            <header class="h-20 bg-white/80 backdrop-blur-md border-b border-surface flex items-center justify-between px-8 sticky top-0 z-40 shrink-0">
                <!-- Breadcrumbs & Search -->
                <div class="flex items-center gap-8">
                    <div class="flex items-center gap-2 text-xs font-bold text-text-sub">
                        <span data-i18n="NAV_BREADCRUMB_HOME">主页</span>
                        <span class="material-symbols-outlined text-[10px]">chevron_right</span>
                        <span class="text-primary">${title}</span>
                    </div>
                    <!-- Search Bar -->
                    <div class="relative w-80 group">
                        <span class="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-text-sub text-lg group-focus-within:text-primary transition-colors">search</span>
                        <input type="text" placeholder="输入订单号或关键词搜索..." data-i18n-placeholder="LBL_SEARCH_PLACEHOLDER"
                            class="w-full bg-base border-none rounded-full py-2.5 pl-12 pr-6 text-xs font-medium focus:ring-1 focus:ring-primary/20 transition-all">
                    </div>
                </div>

                <!-- Header Actions -->
                <div class="flex items-center gap-6">
                    <!-- Notifications -->
                    <button class="relative w-10 h-10 bg-base rounded-full flex items-center justify-center hover:bg-surface transition-colors cursor-pointer group">
                        <span class="material-symbols-outlined text-text-sub group-hover:text-primary transition-colors">notifications</span>
                        <span class="absolute top-2.5 right-2.5 w-2 h-2 bg-primary rounded-full border-2 border-white"></span>
                    </button>

                    <!-- Language Selector -->
                    <div class="lang-selector border border-surface bg-base px-3 py-1.5 rounded-full scale-90">
                        <span class="lang-btn" onclick="switchLanguage('zh')">简</span>
                        <span class="lang-btn" onclick="switchLanguage('zt')">繁</span>
                        <span class="lang-btn" onclick="switchLanguage('ja')">日</span>
                        <span class="lang-btn" onclick="switchLanguage('en')">EN</span>
                    </div>

                    <!-- Vertical Divider -->
                    <div class="w-px h-8 bg-surface mx-2"></div>

                    <!-- Profile Info -->
                    <div class="flex items-center gap-3 hover:bg-base p-2 rounded-xl transition-all cursor-pointer">
                        <div class="text-right hidden xl:block">
                            <p class="text-xs font-black">Sakura.Y</p>
                            <p class="text-[10px] text-text-sub font-bold uppercase tracking-wider">Super Merchant</p>
                        </div>
                        <img src="https://i.pravatar.cc/100?u=merchant" class="w-10 h-10 rounded-xl object-cover ring-2 ring-surface">
                    </div>
                </div>
            </header>
        `,
        footer: (pageId) => `
            <footer class="h-12 bg-white border-t border-surface flex items-center justify-between px-8 text-[10px] font-bold text-text-sub uppercase tracking-wider shrink-0">
                <p>© 2024 BOBBUY GLOBAL. ALL RIGHTS RESERVED.</p>
                <div class="flex gap-4">
                    <a href="#" class="hover:text-primary">Support</a>
                    <a href="#" class="hover:text-primary">API Docs</a>
                    <a href="#" class="hover:text-primary opacity-50">${pageId}</a>
                </div>
            </footer>
        `
    },

    init: function () {
        const body = document.body;
        const pageId = body.getAttribute('data-page-id') || 'UID_UNKNOWN';
        const pageTitle = body.getAttribute('data-page-title') || 'Merchant Console';

        // 1. Inject HTML Components
        this.injectComponents(pageId, pageTitle);

        // 2. Setup Active Navigation Link
        this.setupNavigation(pageId);

        // 3. Setup Toggles
        this.setupToggles();

        // 4. Trigger localized updates if ui-core global function exists
        if (window.updateTranslations) {
            window.updateTranslations();
            window.updateLangButtons();
        }
    },

    injectComponents: function (pageId, pageTitle) {
        // Find or create layout structure
        let appContainer = document.getElementById('app-layout');
        if (!appContainer) {
            // If the page doesn't have the standard app-layout wrapper, use the default structure
            console.warn('Framework: #app-layout container not found. Injected at body start.');
        }

        const sidebarPlaceholder = document.querySelector('aside-placeholder');
        if (sidebarPlaceholder) {
            sidebarPlaceholder.outerHTML = this.templates.sidebar;
        }

        const headerPlaceholder = document.querySelector('header-placeholder');
        if (headerPlaceholder) {
            headerPlaceholder.outerHTML = this.templates.header(pageTitle);
        }

        const footerPlaceholder = document.querySelector('footer-placeholder');
        if (footerPlaceholder) {
            footerPlaceholder.outerHTML = this.templates.footer(pageId);
        }
    },

    setupNavigation: function (currentPageId) {
        const navItems = document.querySelectorAll('.sidebar-item');
        navItems.forEach(item => {
            if (item.getAttribute('data-page') === currentPageId) {
                item.classList.add('active');
            } else {
                item.classList.remove('active');
            }
        });
    },

    setupToggles: function () {
        const toggleBtn = document.getElementById('sidebar-toggle');
        const sidebar = document.getElementById('main-sidebar');

        if (toggleBtn && sidebar) {
            toggleBtn.addEventListener('click', () => {
                const isMini = sidebar.classList.toggle('sidebar-mini');
                sidebar.classList.toggle('w-64');
                localStorage.setItem('merchant_sidebar_collapsed', isMini);

                // Update toggle icon rotation
                const icon = toggleBtn.querySelector('.material-symbols-outlined');
                if (icon) {
                    icon.classList.toggle('rotate-180', isMini);
                }

                // Update toggle button text
                const text = toggleBtn.querySelector('.sidebar-text');
                if (text) {
                    text.textContent = isMini ? '' : '收起导航';
                }
            });
        }
    }
};

// Initialize on load
document.addEventListener('DOMContentLoaded', () => {
    // Small delay to ensure ui-core.js init hasn't wiped data-i18n if it runs first
    setTimeout(() => MERCHANT_FRAMEWORK.init(), 10);
});
