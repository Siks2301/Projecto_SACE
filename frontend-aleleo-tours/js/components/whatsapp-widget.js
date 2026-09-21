/* ==========================================================================
   AleLeo Tours - whatsapp-widget.js
   Módulo independiente opcional para el Botón Flotante de WhatsApp
   ========================================================================== */

'use strict';

(function () {
  const NUMERO_WHATSAPP = '573122149645';
  const MENSAJE_PREDETERMINADO = 'Hola AleLeo Tours, deseo cotizar un plan todo incluido';

  function inyectarEstilosWhatsApp() {
    if (document.getElementById('whatsapp-widget-styles')) return;

    const style = document.createElement('style');
    style.id = 'whatsapp-widget-styles';
    style.textContent = `
      .whatsapp-toggle-btn {
        position: fixed;
        bottom: 24px;
        right: 100px;
        width: 64px;
        height: 64px;
        border-radius: 50%;
        background: #25D366;
        color: #ffffff;
        border: 2px solid #ffffff;
        box-shadow: 0 10px 28px rgba(37, 211, 102, 0.45);
        cursor: pointer;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 32px;
        z-index: 9998;
        transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
        text-decoration: none;
      }
      .whatsapp-toggle-btn:hover {
        transform: scale(1.08) translateY(-4px);
        box-shadow: 0 14px 34px rgba(37, 211, 102, 0.65);
        color: #ffffff;
      }
      @media (max-width: 768px) {
        .whatsapp-toggle-btn {
          bottom: 20px;
          right: 90px;
          width: 56px;
          height: 56px;
          font-size: 28px;
        }
      }
    `;
    document.head.appendChild(style);
  }

  function inyectarBotonWhatsApp() {
    if (document.getElementById('whatsapp-float-btn')) return;

    inyectarEstilosWhatsApp();

    const btnWa = document.createElement('a');
    btnWa.id = 'whatsapp-float-btn';
    btnWa.className = 'whatsapp-toggle-btn';
    btnWa.href = `https://wa.me/${NUMERO_WHATSAPP}?text=${encodeURIComponent(MENSAJE_PREDETERMINADO)}`;
    btnWa.target = '_blank';
    btnWa.rel = 'noopener noreferrer';
    btnWa.setAttribute('aria-label', 'Chatear por WhatsApp con un Asesor de Viajes');
    btnWa.title = 'Chatear por WhatsApp con un Asesor de Viajes';
    btnWa.innerHTML = '<i class="bi bi-whatsapp"></i>';

    document.body.appendChild(btnWa);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', inyectarBotonWhatsApp, { once: true });
  } else {
    inyectarBotonWhatsApp();
  }
})();
