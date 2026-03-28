function fallback() {
  return {
    request: ({ url, method = "GET", data }) =>
      fetch(url, {
        method,
        headers: { "Content-Type": "application/json" },
        body: method === "GET" ? undefined : JSON.stringify(data || {})
      }).then(async (res) => ({
        statusCode: res.status,
        data: await res.json().catch(() => null)
      })),
    login: () => Promise.resolve({ code: "local-dev-code" }),
    showToast: ({ title }) => {
      console.log("[toast]", title);
      return Promise.resolve({ errMsg: "showToast:ok" });
    },
    setStorage: ({ key, data }) => {
      localStorage.setItem(key, JSON.stringify(data));
      return Promise.resolve({ errMsg: "setStorage:ok" });
    },
    getStorage: ({ key }) => {
      const raw = localStorage.getItem(key);
      return Promise.resolve({
        errMsg: "getStorage:ok",
        data: raw ? JSON.parse(raw) : null
      });
    }
  };
}

export const bridge = (window.wx && typeof window.wx.request === "function")
  ? window.wx
  : fallback();

