export function loadReCaptcha(siteKey) {
  return new Promise((resolve, reject) => {
    if (window.grecaptcha) {
      resolve(window.grecaptcha);
      return;
    }
    const script = document.createElement('script');
    script.src = `https://www.google.com/recaptcha/api.js?render=${siteKey}`;
    script.async = true;
    script.defer = true;
    script.onload = () => {
      window.grecaptcha.ready(() => {
        resolve(window.grecaptcha);
      });
    };
    script.onerror = (err) => reject(err);
    document.head.appendChild(script);
  });
}

export async function executeReCaptcha(siteKey, action) {
  try {
    const grecaptcha = await loadReCaptcha(siteKey);
    return await grecaptcha.execute(siteKey, { action });
  } catch (error) {
    console.error('reCAPTCHA execution failed', error);
    return null;
  }
}
