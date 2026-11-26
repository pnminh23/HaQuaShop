// Dev server chuyển /api và /uploads sang backend, nên chỉ cần public cổng 4200.
const target = process.env.BACKEND_URL ?? 'http://localhost:8080';

export default {
  '/api': {
    target,
    // Request cùng nguồn với trang web thì bỏ header Origin, để backend không chặn CORS khi domain là link tunnel.
    // Request từ trang web khác vẫn giữ Origin và vẫn bị CORS_ORIGINS kiểm tra.
    configure: (proxy) => {
      proxy.on('proxyReq', (proxyReq, req) => {
        if (req.headers['sec-fetch-site'] === 'same-origin') proxyReq.removeHeader('origin');
      });
    },
  },
  '/uploads': { target },
};
