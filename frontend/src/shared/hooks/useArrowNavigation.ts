import { useEffect } from 'react';

/**
 * Enables arrow key navigation (Up/Down/Left/Right) to move focus between interactive elements.
 * Acts as a supplement to Tab/Shift+Tab.
 * 
 * Safely ignores:
 * - Inputs/Textareas where cursor movement is needed
 * - Elements that already handle arrow keys (like Radix UI components)
 */
// Hook implementation starts here
// This hook provides global arrow key navigation support.
// While standard accessibility guidelines recommend Tab/Shift+Tab,
// arrow key navigation is a common expectation for dashboard-like interfaces.
// We include strict safeguards to avoid interfering with native input behavior.
export function useArrowNavigation() {
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      // 1. Ignore if modifier keys are pressed (Ctrl, Alt, Shift, Meta)
      if (e.ctrlKey || e.altKey || e.shiftKey || e.metaKey) return;

      // 2. Only handle Arrow keys
      if (!['ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight'].includes(e.key)) return;

      const activeElement = document.activeElement as HTMLElement;

      // 3. SAFETY CHECK: Ignore if we are inside a text input, textarea, or contenteditable
      // We must let the user move the cursor inside these.
      if (activeElement.tagName === 'INPUT') {
        // Exception: If the input is type="button/submit/reset/checkbox/image", arrows are possibly safe.
        // Radio buttons use arrows for selection, so we must ignore them too.
        // Checkboxes use space to toggle, so arrows are okay, but we explicitly allow them here.
        const inputType = (activeElement as HTMLInputElement).type;
        if (!['button', 'submit', 'reset', 'image', 'checkbox'].includes(inputType)) {
          return;
        }
      } else if (
        activeElement.tagName === 'TEXTAREA' ||
        activeElement.isContentEditable ||
        activeElement.tagName === 'SELECT' // Select uses arrows to change options
      ) {
        return;
      }

      // 4. SAFETY CHECK: Ignore if the element is part of a complex widget that handles its own arrows
      // (e.g., Radix UI Menus, Tabs, Radio Groups, Sliders)
      // We detect this by checking for specific ARIA roles or data attributes often used by libraries.
      const role = activeElement.getAttribute('role');
      if (['menuitem', 'option', 'tab', 'radio', 'slider', 'listbox', 'gridcell', 'treeitem'].includes(role || '')) {
        return;
      }
      if (activeElement.closest('[data-radix-collection-item]')) {
        return;
      }

      // 5. Get all focusable elements
      const selector = 'a[href], button, input, textarea, select, details, [tabindex]:not([tabindex="-1"])';
      const focusables = Array.from(document.querySelectorAll(selector))
        .filter(el => {
          const element = el as HTMLElement;
          return !element.hasAttribute('disabled') && 
                 element.getAttribute('aria-hidden') !== 'true' && 
                 element.offsetParent !== null; // Visible only
        }) as HTMLElement[];

      const currentIndex = focusables.indexOf(activeElement);
      let nextIndex = -1;

      // 6. Navigate
      if (e.key === 'ArrowDown' || e.key === 'ArrowRight') {
        nextIndex = currentIndex + 1;
        if (nextIndex >= focusables.length) nextIndex = 0; // Loop to start
      } else if (e.key === 'ArrowUp' || e.key === 'ArrowLeft') {
        nextIndex = currentIndex - 1;
        if (nextIndex < 0) nextIndex = focusables.length - 1; // Loop to end
      }

      if (nextIndex !== -1 && focusables[nextIndex]) {
        e.preventDefault(); // Prevent scrolling
        focusables[nextIndex].focus();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, []);
}
