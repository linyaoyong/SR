import '@testing-library/jest-dom/vitest';

// jsdom 未实现 matchMedia，antd 的 ResponsiveObserve / Grid 依赖它。
// 提供一个空实现，使 Table / Form 等组件在测试环境可正常渲染。
if (!window.matchMedia) {
  Object.defineProperty(window, 'matchMedia', {
    writable: true,
    value: (query: string) => ({
      matches: false,
      media: query,
      onchange: null,
      addListener: () => {},
      removeListener: () => {},
      addEventListener: () => {},
      removeEventListener: () => {},
      dispatchEvent: () => false,
    }),
  });
}

// jsdom 未实现 ResizeObserver，antd Table / Modal 等可能依赖。
if (!window.ResizeObserver) {
  Object.defineProperty(window, 'ResizeObserver', {
    writable: true,
    value: class {
      observe() {}
      unobserve() {}
      disconnect() {}
    },
  });
}

// jsdom 不支持 getComputedStyle 的伪元素参数（rc-table / rc-dialog 会查询
// ::-webkit-scrollbar 测量滚动条宽度）。这里拦截伪元素分支，返回空样式，
// 避免抛出 "Not implemented" 错误导致组件 layout effect 中断。
const originalGetComputedStyle = window.getComputedStyle.bind(window);
window.getComputedStyle = ((elt: Element, pseudoElt?: string | null) => {
  if (pseudoElt) {
    return new Proxy(
      {},
      {
        get: (_t, prop) => {
          if (prop === 'length') return 0;
          if (prop === 'getPropertyValue') return () => '';
          if (prop === 'item') return () => '';
          if (typeof prop === 'string') return '';
          return undefined;
        },
      },
    ) as CSSStyleDeclaration;
  }
  return originalGetComputedStyle(elt);
}) as typeof window.getComputedStyle;
