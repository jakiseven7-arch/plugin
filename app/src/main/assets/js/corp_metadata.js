// corp_metadata.js - Injetado em todas as paginas para padronizar fingerprint.
(function () {
  'use strict';

  var PROFILE = {
    lat: -28.9309,
    lon: -49.0891,
    alt: 10.0,
    ip: "200.150.1.10",
    tz: "America/Sao_Paulo",
    lang: "pt-BR",
    screenW: 390,
    screenH: 844,
    dpr: 3.0
  };

  // --- Stealth de WebView: remove indicios de automacao ---
  try { delete navigator.__proto__.webdriver; } catch (e) {}
  try { Object.defineProperty(navigator, 'webdriver', { get: function () { return false; } }); } catch (e) {}
  try {
    Object.defineProperty(navigator, 'plugins', {
      get: function () { return [1, 2, 3, 4, 5]; }
    });
  } catch (e) {}
  try {
    Object.defineProperty(navigator, 'languages', {
      get: function () { return [PROFILE.lang, 'pt', 'en-US']; }
    });
  } catch (e) {}
  try {
    Object.defineProperty(navigator, 'language', { get: function () { return PROFILE.lang; } });
  } catch (e) {}

  // --- Metadados de sistema ---
  try { Object.defineProperty(screen, 'width', { get: function () { return PROFILE.screenW; } }); } catch (e) {}
  try { Object.defineProperty(screen, 'height', { get: function () { return PROFILE.screenH; } }); } catch (e) {}
  try { Object.defineProperty(window, 'devicePixelRatio', { get: function () { return PROFILE.dpr; } }); } catch (e) {}

  // Intl.DateTimeFormat -> America/Sao_Paulo
  try {
    var _OrigDTF = Intl.DateTimeFormat;
    Intl.DateTimeFormat = function (locales, options) {
      options = options || {};
      options.timeZone = PROFILE.tz;
      return new _OrigDTF(locales || PROFILE.lang, options);
    };
    Intl.DateTimeFormat.supportedLocalesOf = _OrigDTF.supportedLocalesOf;
  } catch (e) {}

  // --- Geo-Metadata: sobrescreve navigator.geolocation ---
  var fakePosition = {
    coords: {
      latitude: PROFILE.lat,
      longitude: PROFILE.lon,
      altitude: PROFILE.alt,
      accuracy: 5,
      altitudeAccuracy: 1,
      heading: null,
      speed: null
    },
    timestamp: Date.now()
  };

  if (navigator.geolocation) {
    navigator.geolocation.getCurrentPosition = function (success, error, options) {
      if (typeof success === 'function') setTimeout(function () { success(fakePosition); }, 0);
    };
    navigator.geolocation.watchPosition = function (success, error, options) {
      if (typeof success === 'function') setTimeout(function () { success(fakePosition); }, 0);
      return 1;
    };
    navigator.geolocation.clearWatch = function () {};
  }

  // --- IP Metadata: intercepta fetch / XHR para servicos de geo-IP ---
  var geoIpPatterns = [
    'ipapi', 'ip-api', 'ipinfo', 'ipgeolocation', 'geoip',
    'geolocation', 'ip.json', 'checkip', 'myip'
  ];

  function isGeoIpUrl(url) {
    if (!url) return false;
    var lower = String(url).toLowerCase();
    return geoIpPatterns.some(function (p) { return lower.indexOf(p) !== -1; });
  }

  function fakeGeoResponse() {
    return JSON.stringify({
      ip: PROFILE.ip,
      city: "Araranguá",
      region: "Santa Catarina",
      region_code: "SC",
      country: "BR",
      country_name: "Brasil",
      latitude: PROFILE.lat,
      longitude: PROFILE.lon,
      timezone: PROFILE.tz,
      org: "CorpSign VPS Ararangua"
    });
  }

  // Intercepta fetch
  if (window.fetch) {
    var origFetch = window.fetch;
    window.fetch = function (input, init) {
      var url = (typeof input === 'string') ? input : (input && input.url);
      if (isGeoIpUrl(url)) {
        return Promise.resolve(new Response(fakeGeoResponse(), {
          status: 200,
          headers: { 'Content-Type': 'application/json' }
        }));
      }
      return origFetch.apply(this, arguments);
    };
  }

  // Intercepta XMLHttpRequest
  var origOpen = XMLHttpRequest.prototype.open;
  var origSend = XMLHttpRequest.prototype.send;
  XMLHttpRequest.prototype.open = function (method, url) {
    this._corpUrl = url;
    return origOpen.apply(this, arguments);
  };
  XMLHttpRequest.prototype.send = function () {
    if (isGeoIpUrl(this._corpUrl)) {
      var self = this;
      setTimeout(function () {
        Object.defineProperty(self, 'responseText', { value: fakeGeoResponse(), writable: true });
        Object.defineProperty(self, 'response', { value: fakeGeoResponse(), writable: true });
        self.status = 200;
        self.readyState = 4;
        if (typeof self.onreadystatechange === 'function') self.onreadystatechange();
        if (typeof self.onload === 'function') self.onload();
      }, 0);
      return;
    }
    return origSend.apply(this, arguments);
  };
})();