/**
 * ArvanShare Landing Page Logic
 * - Bilingual (Persian / English) dynamic engine
 * - Theme Switcher (Dark / Light) with persistence
 * - Interactive S3 Feed Simulator with Real-Time Object Tree Inspector
 * - Copy-to-clipboard handlers
 */

// ── 1. TRANSLATION DICTIONARY (FA / EN) ──────────────────────────────────────
const I18N = {
  en: {
    langBtn: "🇮🇷 فارسی",
    themeLight: "☀️ Light",
    themeDark: "🌙 Dark",
    
    // Nav
    navFeatures: "Features",
    navSimulator: "Live Demo",
    navArch: "Architecture",
    navSetup: "Setup Guide",
    navDownload: "Download",
    
    // Hero
    heroPill: "100% Serverless & Backend-Free",
    heroTitlePrefix: "Private Social Network Powered by ",
    heroTitleHighlight: "ArvanCloud S3",
    heroSubtitle: "A private feed designed for your close circle of friends, family, or team. Zero backend servers, zero database costs, direct S3 atomic sync.",
    btnDownloadApk: "📱 Download Android APK",
    btnDownloadExe: "💻 Download Windows App",
    btnExploreDemo: "⚡ Try Live Simulator",
    statServerless: "Serverless",
    statServerlessSub: "100% Direct to S3",
    statDb: "0 Databases",
    statDbSub: "No Backend Overhead",
    statOffline: "Offline-First",
    statOfflineSub: "Room DB Cached",

    // Simulator
    simTag: "Interactive Simulation",
    simTitle: "Experience ArvanShare in Action",
    simDesc: "Post a message or like a post below. Watch the right panel update with real-time atomic S3 object keys — no database required!",
    simActiveUser: "Active User:",
    simPlaceholder: "What's on your mind? Share with your bucket circle...",
    simBtnPost: "Post to S3",
    simS3Header: "☁️ Simulated S3 Bucket Explorer",
    simS3Sub: "Objects mapped directly in bucket storage",
    simComments: "Comments",
    simLikes: "Likes",
    simLikeAction: "Like",
    simLikedAction: "Liked",

    // Features
    featTag: "Why ArvanShare?",
    featTitle: "Engineered for Privacy & Zero Maintenance",
    featDesc: "By turning ArvanCloud Object Storage into a decentralized social engine, ArvanShare eliminates backend hosting headaches entirely.",
    
    f1Title: "100% Serverless",
    f1Desc: "No Node.js, PHP, or Python backend to manage, patch, or pay for. All interactions are signed direct S3 requests.",
    
    f2Title: "Cross-Platform Ecosystem",
    f2Desc: "Beautiful Jetpack Compose native Android app, modern Windows Tkinter desktop client, and an automation-ready Python CLI.",
    
    f3Title: "Race-Condition Safe",
    f3Desc: "Likes and comments use atomic file markers (e.g. like_ali.txt). Multiple concurrent writes will never corrupt social data.",
    
    f4Title: "Offline-First Architecture",
    f4Desc: "Android app caches all feed metadata locally in Room DB. The feed opens instantly even with unstable connections.",
    
    f5Title: "Strict Circle Privacy",
    f5Desc: "Access is secured via HMAC-SHA256 Bucket API Keys. Only people with bucket keys can see or post updates.",
    
    f6Title: "Polished Themes",
    f6Desc: "Deep indigo and vibrant cyan color schemes with native Dark Mode support out-of-the-box across all platforms.",

    // Architecture
    archTag: "Under The Hood",
    archTitle: "Direct-to-S3 Data Architecture",
    archDesc: "How posts, comments, likes, and media attachments map cleanly to object keys without any central database.",
    archP1Title: "📁 JSON Posts & Media",
    archP1Desc: "Every post is an immutable JSON file with its corresponding high-res media uploaded to /posts/.",
    archP2Title: "💬 Scoped Comments",
    archP2Desc: "Comments live in dedicated subdirectories named after the post ID (/comments/<post_id>/).",
    archP3Title: "🛡️ Atomic Like Markers",
    archP3Desc: "A like is simply an empty marker file. S3 PutObject and DeleteObject are atomic operations.",

    // Setup
    setupTag: "Easy Setup",
    setupTitle: "4 Steps to Launch Your Private Circle",
    setupDesc: "Setting up an ArvanCloud bucket takes less than 2 minutes. Share the credentials with your team and start posting.",
    step1Num: "1",
    step1Title: "Create Private Bucket",
    step1Desc: "Go to panel.arvancloud.ir -> Object Storage, click 'New Bucket', set access level to Private.",
    step2Num: "2",
    step2Title: "Get S3 Endpoint",
    step2Desc: "Copy your regional endpoint URL (e.g. https://s3.ir-thr-at1.arvanstorage.ir).",
    step3Num: "3",
    step3Title: "Generate API Keys",
    step3Desc: "Create a Read/Write API Key in the panel and attach it directly to your bucket.",
    step4Num: "4",
    step4Title: "Enter into App",
    step4Desc: "Give the 4 details (Bucket, Endpoint, Access Key, Secret Key) to your circle.",

    // Downloads
    downTag: "Get Started",
    downTitle: "Download Clients for Any Device",
    downDesc: "Get the latest signed APK for Android, standalone portable EXE for Windows, or run via Python.",
    dAndroidTitle: "Android APK",
    dAndroidDesc: "Native Kotlin & Jetpack Compose. Requires Android 8.0+.",
    dAndroidBtn: "Download .APK",
    dWinTitle: "Windows Desktop",
    dWinDesc: "Portable standalone .EXE. No Python installation required.",
    dWinBtn: "Download .EXE",
    dCliTitle: "Python CLI",
    dCliDesc: "Reference CLI & automation tool. Tested with Python 3.10+.",
    dCliBtn: "View CLI Guide",
    cliRunCmd: "python -m pip install -r requirements.txt && python arvanshare.py list-posts",
    copyBtn: "Copy Command",
    copiedText: "Copied!",

    // Footer
    footerDesc: "A serverless, private social feed designed for close circles, powered by ArvanCloud S3 Object Storage.",
    footerDev: "Created with ❤️ by HasanMfar",
    footerLicense: "Released under MIT License",
  },

  fa: {
    langBtn: "🇺🇸 English",
    themeLight: "☀️ روشن",
    themeDark: "🌙 تاریک",

    // Nav
    navFeatures: "ویژگی‌ها",
    navSimulator: "دموی آنلاین",
    navArch: "معماری ابری",
    navSetup: "راهنمای راه‌اندازی",
    navDownload: "دانلود برنامه",

    // Hero
    heroPill: "۱۰۰٪ بدون سرور (Serverless) و بدون دیتابیس",
    heroTitlePrefix: "شبکه اجتماعی خصوصی قدرت گرفته از ",
    heroTitleHighlight: "فضای ابری آروان‌کلاد",
    heroSubtitle: "یک فید اختصاصی و صمیمی برای دوستان، خانواده یا تیم کاری شما. بدون نیاز به سرور بک‌اند، بدون هزینه نگهداری دیتابیس، با همگام‌سازی مستقیم و اتمیک روی باکت S3.",
    btnDownloadApk: "📱 دانلود نسخه اندروید (APK)",
    btnDownloadExe: "💻 دانلود نسخه ویندوز (EXE)",
    btnExploreDemo: "⚡ اجرای شبیه‌ساز آنلاین",
    statServerless: "Serverless",
    statServerlessSub: "اتصال ۱۰۰٪ مستقیم به S3",
    statDb: "۰ دیتابیس",
    statDbSub: "بدون بار و سرور بک‌اند",
    statOffline: "آفلاین-فرست",
    statOfflineSub: "کش سریع با Room DB",

    // Simulator
    simTag: "شبیه‌ساز تعاملی",
    simTitle: "عملکرد آروان‌شیر را زنده تجربه کنید",
    simDesc: "یک پیام بفرستید یا پستی را لایک کنید. در پنل روبرو مشاهده کنید که چگونه کلیدهای اتمیک S3 بدون نیاز به هیچ دیتابیسی به‌روزرسانی می‌شوند!",
    simActiveUser: "کاربر فعال:",
    simPlaceholder: "به چه چیزی فکر می‌کنید؟ در صندوقچه اختصاصی خود به اشتراک بگذارید...",
    simBtnPost: "ارسال به S3",
    simS3Header: "☁️ کاوشگر باکت فضای ابری S3",
    simS3Sub: "فایل‌ها و کلیدهای ذخیره شده روی آروان‌کلاد",
    simComments: "نظر",
    simLikes: "لایک",
    simLikeAction: "لایک",
    simLikedAction: "لایک شد",

    // Features
    featTag: "چرا آروان‌شیر؟",
    featTitle: "طراحی شده برای امنیت، حریم خصوصی و نگهداری صفر",
    featDesc: "با تبدیل فضای ذخیره‌سازی ابری آروان به یک موتور اجتماعی توزیع‌شده، دغدغه‌های مربوط به هاست، کرش کردن سرور و نشت دیتابیس به کلی حذف شده است.",

    f1Title: "۱۰۰٪ بدون سرور (Serverless)",
    f1Desc: "بدون نیاز به بک‌اند Node.js، PHP یا جنگو. تمام درخواست‌ها به صورت مستقیم و امضا شده با کلید S3 انجام می‌شوند.",

    f2Title: "پوشش چند پلتفرمی کامل",
    f2Desc: "دارای اپلیکیشن نیتیو اندروید با Jetpack Compose، کلاینت پرتابل ویندوز، و ابزار اسکریپت‌نویسی خط فرمان (CLI).",

    f3Title: "ایمن در برابر تداخل (Race-Safe)",
    f3Desc: "لایک‌ها و کامنت‌ها فایل‌های اتمیک جداگانه دارند (مانند like_ali.txt)؛ بنابراین ارسال همزمان چند کاربر تداخلی ایجاد نمی‌کند.",

    f4Title: "معماری آفلاین-فرست (Offline-First)",
    f4Desc: "اپلیکیشن اندروید تمام داده‌ها را در دیتابیس Room گوشی کش می‌کند تا در هر شرایطی فید به سرعت باز شود.",

    f5Title: "حریم خصوصی اختصاصی حلقه",
    f5Desc: "دسترسی تنها با کلیدهای API باکت با امضای HMAC-SHA256 انجام می‌گیرد و داده‌ها خارج از باکت شما درز نمی‌کند.",

    f6Title: "پوسته‌های دارک و لایت مدرن",
    f6Desc: "رنگ‌بندی ایندیگو و فیروزه‌ای چشم‌نواز با پشتیبانی پیش‌فرض از دارک مود در تمام کلاینت‌ها.",

    // Architecture
    archTag: "زیر ذره‌بین مهندسی",
    archTitle: "معماری مستقیم به S3 بدون دیتابیس",
    archDesc: "چگونگی چینش پست‌ها، پیوست‌های رسانه‌ای، کامنت‌ها و لایک‌ها در باکت آبجکت استوریج آروان‌کلاد.",
    archP1Title: "📁 فایل‌های JSON و تصاویر",
    archP1Desc: "هر پست یک فایل JSON مستقل همراه با فایل تصویر باکیفیت در پوشه /posts/ ذخیره می‌شود.",
    archP2Title: "💬 کامنت‌های تفکیک شده",
    archP2Desc: "کامنت‌های هر پست در یک پوشه اختصاصی با شناسه همان پست (/comments/<post_id>/) ذخیره می‌شوند.",
    archP3Title: "🛡️ نشانگرهای لایک اتمیک",
    archP3Desc: "هر لایک یک فایل متنی سبک است. متدهای PutObject و DeleteObject در S3 اتمیک و بدون تداخل هستند.",

    // Setup
    setupTag: "راه‌اندازی ساده",
    setupTitle: "۴ گام برای ساخت شبکه خصوصی شما",
    setupDesc: "ساخت باکت در پنل آروان‌کلاد کمتر از ۲ دقیقه زمان می‌برد. مشخصات را به اعضای گروه بدهید و استفاده کنید.",
    step1Num: "۱",
    step1Title: "ساخت صندوقچه خصوصی",
    step1Desc: "به پنل آروان‌کلاد -> فضای ابری رفته، روی صندوقچه جدید کلیک کرده و دسترسی را خصوصی بگذارید.",
    step2Num: "۲",
    step2Title: "دریافت آدرس S3 Endpoint",
    step2Desc: "آدرس اندپوینت منطقه خود را کپی کنید (مثلاً https://s3.ir-thr-at1.arvanstorage.ir).",
    step3Num: "۳",
    step3Title: "ساخت کلیدهای API",
    step3Desc: "در بخش API Keys یک کلید خواندن و نوشتن ساخته و آن را به صندوقچه متصل (Attach) کنید.",
    step4Num: "۴",
    step4Title: "ورود اطلاعات در برنامه",
    step4Desc: "این ۴ مشخصه (نام باکت، اندپوینت، Access Key، Secret Key) را در اپلیکیشن وارد کنید.",

    // Downloads
    downTag: "دریافت برنامه",
    downTitle: "دانلود کلاینت‌های آروان‌شیر",
    downDesc: "دانلود مستقیم فایل APK امضا شده برای اندروید، نسخه پرتابل ویندوز یا اجرای ابزار پایتون.",
    dAndroidTitle: "اپلیکیشن اندروید",
    dAndroidDesc: "توسعه داده شده با Kotlin و Jetpack Compose. سازگار با اندروید ۸ به بالا.",
    dAndroidBtn: "دانلود فایل APK",
    dWinTitle: "نسخه دسکتاپ ویندوز",
    dWinDesc: "فایل پرتابل .EXE بدون نیاز به نصب پایتون و ابزارهای جانبی.",
    dWinBtn: "دانلود فایل EXE",
    dCliTitle: "خط فرمان (CLI)",
    dCliDesc: "ابزار پایتون جهت خودکارسازی و مدیریت فید. نیازمند پایتون ۳.۱۰+.",
    dCliBtn: "مشاهده راهنمای CLI",
    cliRunCmd: "python -m pip install -r requirements.txt && python arvanshare.py list-posts",
    copyBtn: "کپی دستور",
    copiedText: "کپی شد!",

    // Footer
    footerDesc: "شبکه اجتماعی خصوصی و بدون سرور برای حلقه‌های صمیمی، قدرت گرفته از فضای ابری آروان‌کلاد.",
    footerDev: "توسعه داده شده با ❤️ توسط HasanMfar",
    footerLicense: "تحت مجوز متن‌باز MIT",
  }
};

// ── 2. STATE & SIMULATION DATA ──────────────────────────────────────────────
let currentLang = localStorage.getItem("arvanshare_lang") || "fa";
let currentTheme = localStorage.getItem("arvanshare_theme") || "dark";
let currentUser = "Ali";

const USERS = {
  Ali: { color: "#5C6BC0", initial: "A", name: "Ali" },
  Sara: { color: "#EC407A", initial: "S", name: "Sara" },
  Reza: { color: "#26A69A", initial: "R", name: "Reza" },
  Hasan: { color: "#00BCD4", initial: "H", name: "Hasan" }
};

let simPosts = [
  {
    id: "20260902_114500_sara",
    author: "Sara",
    time: "10 mins ago",
    text: "Updated the architectural specifications. Atomic like markers eliminate race conditions completely! ☁️✨",
    likes: ["Ali", "Reza", "Hasan"],
    comments: [
      { author: "Reza", text: "Awesome! Direct S3 calls are blazing fast." }
    ]
  },
  {
    id: "20260902_111000_ali",
    author: "Ali",
    time: "45 mins ago",
    text: "Loving the new Material 3 theme and offline cache with Room DB on Android! 🚀",
    likes: ["Sara", "Hasan"],
    comments: [
      { author: "Hasan", text: "Zero backend maintenance is a game changer." }
    ]
  }
];

// ── 3. LANGUAGE & THEME ENGINE ───────────────────────────────────────────────
function updateLanguage(lang) {
  currentLang = lang;
  localStorage.setItem("arvanshare_lang", lang);
  
  const dict = I18N[lang];
  document.documentElement.lang = lang;
  document.body.dir = lang === "fa" ? "rtl" : "ltr";

  // Translate all [data-i18n] elements
  document.querySelectorAll("[data-i18n]").forEach(el => {
    const key = el.getAttribute("data-i18n");
    if (dict[key]) {
      el.textContent = dict[key];
    }
  });

  // Translate all [data-i18n-ph] (placeholders)
  document.querySelectorAll("[data-i18n-ph]").forEach(el => {
    const key = el.getAttribute("data-i18n-ph");
    if (dict[key]) {
      el.setAttribute("placeholder", dict[key]);
    }
  });

  // Update Language Button Text
  const langToggleBtn = document.getElementById("lang-toggle-btn");
  if (langToggleBtn) {
    langToggleBtn.innerHTML = `<span>🌐</span> ${dict.langBtn}`;
  }

  renderSimulatorFeed();
  renderS3Tree();
}

function updateTheme(theme) {
  currentTheme = theme;
  localStorage.setItem("arvanshare_theme", theme);
  document.documentElement.setAttribute("data-theme", theme);

  const themeToggleBtn = document.getElementById("theme-toggle-btn");
  if (themeToggleBtn) {
    const isDark = theme === "dark";
    themeToggleBtn.innerHTML = `<span>${isDark ? "🌙" : "☀️"}</span> <span>${isDark ? I18N[currentLang].themeDark : I18N[currentLang].themeLight}</span>`;
  }
}

// ── 4. SIMULATOR LOGIC & RENDERING ──────────────────────────────────────────
function renderSimulatorFeed() {
  const container = document.getElementById("sim-feed-container");
  if (!container) return;

  container.innerHTML = "";

  simPosts.forEach(post => {
    const userMeta = USERS[post.author] || { color: "#5C6BC0", initial: post.author.charAt(0) };
    const hasLiked = post.likes.includes(currentUser);

    const postEl = document.createElement("div");
    postEl.className = "sim-post";
    postEl.innerHTML = `
      <div class="sim-post-author-row">
        <div class="sim-avatar" style="background-color: ${userMeta.color}">
          ${userMeta.initial}
        </div>
        <div>
          <div class="sim-author-name">${post.author}</div>
          <div class="sim-post-time">${post.time}</div>
        </div>
      </div>
      <div class="sim-post-text">${escapeHtml(post.text)}</div>
      <div class="sim-post-actions">
        <button class="sim-act-btn ${hasLiked ? 'liked' : ''}" onclick="toggleSimLike('${post.id}')">
          <span>${hasLiked ? '❤️' : '🤍'}</span>
          <span>${post.likes.length} ${I18N[currentLang].simLikes}</span>
        </button>
        <button class="sim-act-btn" style="cursor: default;">
          <span>💬</span>
          <span>${post.comments.length} ${I18N[currentLang].simComments}</span>
        </button>
      </div>
    `;
    container.appendChild(postEl);
  });
}

function renderS3Tree() {
  const treeContainer = document.getElementById("s3-tree-view");
  if (!treeContainer) return;

  let html = `
    <div class="s3-tree-item">
      <span class="s3-folder">📁 / (arvanshare-bucket)</span>
    </div>
    <div class="s3-tree-item" style="padding-inline-start: 20px;">
      <span class="s3-folder">📁 posts/</span>
    </div>
  `;

  // List all post JSON files
  simPosts.forEach(p => {
    html += `
      <div class="s3-tree-item" style="padding-inline-start: 40px;">
        <span class="s3-file">📄 ${p.id}_post.json</span>
        <span class="s3-meta-tag">${p.author} • ${p.text.length} B</span>
      </div>
    `;
  });

  // Comments folder
  html += `
    <div class="s3-tree-item" style="padding-inline-start: 20px; margin-top: 8px;">
      <span class="s3-folder">📁 comments/</span>
    </div>
  `;
  simPosts.forEach(p => {
    if (p.comments.length > 0) {
      html += `
        <div class="s3-tree-item" style="padding-inline-start: 40px;">
          <span class="s3-folder">📁 ${p.id}/</span>
        </div>
      `;
      p.comments.forEach((c, idx) => {
        html += `
          <div class="s3-tree-item" style="padding-inline-start: 60px;">
            <span class="s3-file">📄 comment_${idx + 1}_${c.author.toLowerCase()}.json</span>
          </div>
        `;
      });
    }
  });

  // Likes marker folder
  html += `
    <div class="s3-tree-item" style="padding-inline-start: 20px; margin-top: 8px;">
      <span class="s3-folder" style="color: #EF5350;">📁 likes/ (Atomic Markers)</span>
    </div>
  `;
  simPosts.forEach(p => {
    if (p.likes.length > 0) {
      html += `
        <div class="s3-tree-item" style="padding-inline-start: 40px;">
          <span class="s3-folder">📁 ${p.id}/</span>
        </div>
      `;
      p.likes.forEach(u => {
        html += `
          <div class="s3-tree-item" style="padding-inline-start: 60px;">
            <span class="s3-like-file">🏷️ like_${u.toLowerCase()}.txt</span>
            <span class="s3-meta-tag">0 B (Atomic)</span>
          </div>
        `;
      });
    }
  });

  treeContainer.innerHTML = html;
}

window.toggleSimLike = function(postId) {
  const post = simPosts.find(p => p.id === postId);
  if (!post) return;

  const idx = post.likes.indexOf(currentUser);
  if (idx > -1) {
    post.likes.splice(idx, 1);
  } else {
    post.likes.push(currentUser);
  }

  renderSimulatorFeed();
  renderS3Tree();
};

function handleSimPost() {
  const input = document.getElementById("sim-post-text");
  if (!input) return;

  const text = input.value.trim();
  if (!text) return;

  const now = new Date();
  const pad = n => String(n).padStart(2, '0');
  const timestampStr = `${now.getFullYear()}${pad(now.getMonth()+1)}${pad(now.getDate())}_${pad(now.getHours())}${pad(now.getMinutes())}${pad(now.getSeconds())}`;
  
  const newPost = {
    id: `${timestampStr}_${currentUser.toLowerCase()}`,
    author: currentUser,
    time: "Just now",
    text: text,
    likes: [currentUser],
    comments: []
  };

  simPosts.unshift(newPost);
  input.value = "";

  renderSimulatorFeed();
  renderS3Tree();
}

function escapeHtml(str) {
  return str.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}

// ── 5. INITIALIZATION & EVENT HANDLERS ───────────────────────────────────────
document.addEventListener("DOMContentLoaded", () => {
  // Initialize theme and language
  updateTheme(currentTheme);
  updateLanguage(currentLang);

  // Theme Toggle Button
  const themeBtn = document.getElementById("theme-toggle-btn");
  if (themeBtn) {
    themeBtn.addEventListener("click", () => {
      const nextTheme = currentTheme === "dark" ? "light" : "dark";
      updateTheme(nextTheme);
    });
  }

  // Language Toggle Button
  const langBtn = document.getElementById("lang-toggle-btn");
  if (langBtn) {
    langBtn.addEventListener("click", () => {
      const nextLang = currentLang === "fa" ? "en" : "fa";
      updateLanguage(nextLang);
    });
  }

  // User Chips in Simulator
  document.querySelectorAll(".user-chip").forEach(chip => {
    chip.addEventListener("click", () => {
      document.querySelectorAll(".user-chip").forEach(c => c.classList.remove("active"));
      chip.classList.add("active");
      currentUser = chip.getAttribute("data-user");
      renderSimulatorFeed();
      renderS3Tree();
    });
  });

  // Post Button in Simulator
  const postBtn = document.getElementById("sim-post-btn");
  if (postBtn) {
    postBtn.addEventListener("click", handleSimPost);
  }

  // Enter key in post textarea (Ctrl/Cmd + Enter to post)
  const postInput = document.getElementById("sim-post-text");
  if (postInput) {
    postInput.addEventListener("keydown", (e) => {
      if (e.key === "Enter" && !e.shiftKey) {
        e.preventDefault();
        handleSimPost();
      }
    });
  }

  // Copy CLI command button
  const copyBtn = document.getElementById("copy-cli-btn");
  if (copyBtn) {
    copyBtn.addEventListener("click", () => {
      const cmd = "python -m pip install -r requirements.txt && python arvanshare.py list-posts";
      navigator.clipboard.writeText(cmd).then(() => {
        const originalText = copyBtn.textContent;
        copyBtn.textContent = I18N[currentLang].copiedText || "Copied!";
        copyBtn.style.background = "var(--success)";
        setTimeout(() => {
          copyBtn.textContent = originalText;
          copyBtn.style.background = "";
        }, 2000);
      });
    });
  }
});
