# Panic Shield — Play Store Listing Copy

App name: **Panic Shield** (verify availability; fallback: "Panic Shield — SOS Lock")
Category: **Tools** (alt: Lifestyle / Personal safety)
Content rating: Everyone / PEGI 3
Default language: English; also localize Turkish.

---

## Short description (≤80 chars)

**EN:** `Volume up x3 to lock, alert a contact, and shield your apps. Fully private.`
(74 chars)

**TR:** `Sesi 3 kez aç: kilitle, yakınına haber ver, uygulamaları koru. Tamamen gizli.`
(77 chars)

---

## Full description (≤4000 chars)

### EN

**Panic Shield turns a discreet gesture into instant protection.**

Press Volume Up three times — even in your pocket — and Panic Shield locks your
phone and runs the safeguards you've armed.

**What it does**
• ⚡ Instant lock — a triple Volume-Up press locks the screen immediately. Set the
  press count (2–5) and timing to suit you.
• 📍 SOS message — automatically text a trusted contact your location (a Google
  Maps link) the moment you trigger.
• 📷 Silent photo — if someone wakes or unlocks your phone while panic is active,
  the front camera quietly saves a photo to your device to help identify them.
• 🔒 Protected apps — banking, wallets, photos, messengers… while panic is active,
  opening a protected app bounces straight back to the home screen.
• 🛡️ Safe-PIN disarm — everything stays locked down until you enter your private
  safe PIN.

**Privacy you can verify**
Panic Shield has no internet permission and no servers. Nothing is ever uploaded.
Your photos, PIN, and settings never leave your phone. The only thing that leaves
your device is the SOS text you trigger yourself, sent through your own carrier to
the contact you chose. No accounts. No tracking. No ads.

**Honest about limits**
• It performs a standard screen lock (Android has no public API to disable
  fingerprint/face unlock for third-party apps).
• App-blocking raises the bar against a snooping thief; it is not an unbreakable
  vault.
• On some phones you may need to allow Autostart and disable battery limits to
  keep the trigger reliable — the app guides you.

Stay safe with a gesture nobody notices.

### TR

**Panic Shield, fark edilmeyen bir hareketi anında korumaya çevirir.**

Sesi açma tuşuna üç kez bas — cebindeyken bile — Panic Shield telefonunu kilitler
ve hazırladığın güvenlik önlemlerini çalıştırır.

**Neler yapar**
• ⚡ Anında kilit — üç kez ses açma ekranı hemen kilitler. Basış sayısını (2–5) ve
  süreyi kendine göre ayarla.
• 📍 SOS mesajı — tetiklediğin an güvendiğin kişiye konumunu (Google Maps bağlantısı)
  otomatik SMS olarak gönderir.
• 📷 Sessiz fotoğraf — panik aktifken biri telefonu uyandırır veya açarsa, ön kamera
  sessizce bir fotoğraf çekip cihazına kaydeder.
• 🔒 Korumalı uygulamalar — banka, cüzdan, galeri, mesajlaşma… panik aktifken
  korumalı bir uygulamayı açmak seni doğrudan ana ekrana geri atar.
• 🛡️ Güvenli PIN ile kapatma — sen özel güvenli PIN'ini girene kadar her şey kilitli
  kalır.

**Doğrulayabileceğin gizlilik**
Panic Shield'in internet izni ve sunucusu yok. Hiçbir şey yüklenmez. Fotoğrafların,
PIN'in ve ayarların telefonundan asla çıkmaz. Cihazından çıkan tek şey, senin
tetiklediğin SOS mesajıdır; kendi operatörünle, senin seçtiğin kişiye gider.
Hesap yok. Takip yok. Reklam yok.

**Sınırlar konusunda dürüst**
• Standart ekran kilidi yapar (Android, üçüncü taraf uygulamalara parmak izi/yüz
  kilidini kapatma izni vermez).
• Uygulama engelleme, meraklı bir hırsıza karşı engel yükseltir; kırılamaz bir kasa
  değildir.
• Bazı telefonlarda tetikleyicinin güvenilir çalışması için Otomatik Başlatma'yı
  açman ve pil kısıtlamasını kaldırman gerekebilir — uygulama yol gösterir.

Kimsenin fark etmediği bir hareketle güvende kal.

---

## ASO keywords

**EN:** panic button, sos, emergency, safety, screen lock, app lock, private, anti-theft, personal safety, lock phone
**TR:** panik butonu, sos, acil durum, güvenlik, ekran kilidi, uygulama kilidi, gizlilik, hırsızlığa karşı, kişisel güvenlik, telefon kilitle

---

## Required assets (to produce)

- [ ] **App icon** 512×512 PNG — export from the in-app shield icon (red shield on
      dark gradient). Source: `res/drawable/ic_launcher_foreground.xml` + background.
- [ ] **Feature graphic** 1024×500 — dark bg, shield emblem left, tagline
      "Volume up ×3. Lock. Alert. Protect." right.
- [ ] **Phone screenshots** (min 2, ideally 5–8), 1080×1920+:
      1. Home — PROTECTED (emerald shield)
      2. Panic active — red DISARM screen
      3. Settings — emergency contact + camera
      4. Protected apps picker
      5. Captured photos gallery
- [ ] Optional 30s promo video — trigger → lock → unlock → photo+SMS+blocked → disarm.

Raw screenshots can be captured from a device via
`adb exec-out screencap -p > shotN.png`, then framed in a mockup tool.

## Pre-submission checklist
- [ ] Privacy Policy URL live (host PRIVACY.md, e.g. GitHub Pages)
- [ ] Data Safety form filled per `docs/data-safety.md`
- [ ] Sensitive permission declarations (Accessibility, SMS, Location, Camera)
- [ ] Demo video if requested for SMS/Accessibility review
