/**
 * BobBuy Hamburger Menu Component
 * Shared navigation drawer for all App pages
 */

// Menu configurations
const MENU_CONFIG = {
    merchant: {
        title: '商户中心',
        subtitle: 'Merchant Portal',
        icon: 'storefront',
        items: [
            { section: '核心功能' },
            { icon: 'dashboard', label: '控制台', href: 'UID1100_merchant_main.html' },
            { icon: 'inventory_2', label: '商品维护', href: 'UID1120_merchants_product_mgmt.html' },
            { icon: 'shopping_bag', label: '订单管理', href: 'UID1100_merchant_main.html' },
            { icon: 'payments', label: '结算记录', href: 'UID1113_merchants_project_settlement.html' },
            { section: '工具与设置' },
            { icon: 'chat', label: '消息中心', href: '#', disabled: true, badge: '即将推出' },
            { icon: 'analytics', label: '数据报告', href: 'UID1116_merchants_project_reports.html' },
            { icon: 'settings', label: '设置', href: '#', disabled: true, badge: '即将推出' },
            { icon: 'person', label: '我的账户', href: 'UID1151_merchant_my.html' }
        ]
    },
    customer: {
        title: '客户中心',
        subtitle: 'Customer Portal',
        icon: 'shopping_bag',
        items: [
            { section: '核心功能' },
            { icon: 'home', label: '首页', href: 'UID1200_customer_main.html' },
            { icon: 'shopping_bag', label: '我的订单', href: '#', disabled: true, badge: '即将推出' },
            { icon: 'chat', label: '对话', href: 'UID1211_customer_project_talk.html' },
            { icon: 'person', label: '我的', href: '#', disabled: true, badge: '即将推出' },
            { section: '帮助与设置' },
            { icon: 'help', label: '帮助中心', href: '#', disabled: true },
            { icon: 'settings', label: '设置', href: '#', disabled: true }
        ]
    }
};

// Initialize hamburger menu
function initHamburgerMenu(type = 'merchant', currentPage = '') {
    const config = MENU_CONFIG[type];
    if (!config) return;

    // Create drawer HTML
    const drawerHTML = `
        <div id="navDrawer" class="fixed inset-0 z-[100] hidden">
            <div class="absolute inset-0 bg-black/40 backdrop-blur-sm" onclick="toggleMenu()"></div>
            <div id="drawerContent" class="absolute left-0 top-0 bottom-0 w-72 bg-white shadow-2xl transform -translate-x-full transition-transform duration-300 ease-out overflow-y-auto">
                <div class="p-6 border-b border-surface flex items-center justify-between">
                    <div class="flex items-center gap-3">
                        <div class="w-10 h-10 rounded-full bg-primary/10 flex items-center justify-center">
                            <span class="material-symbols-outlined text-primary">${config.icon}</span>
                        </div>
                        <div>
                            <h3 class="text-sm font-black">${config.title}</h3>
                            <p class="text-[10px] text-text-sub">${config.subtitle}</p>
                        </div>
                    </div>
                    <button onclick="toggleMenu()" class="w-8 h-8 flex items-center justify-center text-text-sub">
                        <span class="material-symbols-outlined">close</span>
                    </button>
                </div>
                <div class="py-2">
                    ${config.items.map(item => {
        if (item.section) {
            return `<div class="px-4 py-2 ${item !== config.items[0] ? 'mt-4' : ''}">
                                <p class="text-[10px] font-black text-text-sub uppercase tracking-widest">${item.section}</p>
                            </div>`;
        }
        const isActive = currentPage && item.href.includes(currentPage);
        const activeClass = isActive ? 'bg-primary/10 text-primary' : 'hover:bg-surface transition-colors';
        const disabledClass = item.disabled ? 'opacity-50' : '';
        return `<a href="${item.href}" class="flex items-center gap-3 px-6 py-4 ${activeClass} ${disabledClass}">
                            <span class="material-symbols-outlined">${item.icon}</span>
                            <span class="text-sm font-bold">${item.label}</span>
                            ${item.badge ? `<span class="ml-auto text-[10px] text-text-sub">${item.badge}</span>` : ''}
                        </a>`;
    }).join('')}
                </div>
            </div>
        </div>
    `;

    // Inject drawer into DOM
    document.body.insertAdjacentHTML('beforeend', drawerHTML);
}

// Toggle menu function
function toggleMenu() {
    const drawer = document.getElementById('navDrawer');
    const content = document.getElementById('drawerContent');

    if (!drawer || !content) return;

    if (drawer.classList.contains('hidden')) {
        drawer.classList.remove('hidden');
        setTimeout(() => {
            content.classList.remove('-translate-x-full');
        }, 10);
    } else {
        content.classList.add('-translate-x-full');
        setTimeout(() => {
            drawer.classList.add('hidden');
        }, 300);
    }
}

// Auto-initialize on DOM load
if (typeof window !== 'undefined') {
    window.toggleMenu = toggleMenu;
    window.initHamburgerMenu = initHamburgerMenu;
}
