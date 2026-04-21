package com.rps.samaj.auth;

final class EmailTemplates {

    private EmailTemplates() {
    }

    static String otpHtml(String siteName, String otp, int ttlMinutes) {
        String brand = esc(siteName != null && !siteName.isBlank() ? siteName : "Samaj");
        String code = esc(otp != null ? otp : "");
        String ttl = ttlMinutes > 0 ? String.valueOf(ttlMinutes) : "10";

        return """
                <!doctype html>
                <html lang="en">
                  <head>
                    <meta charset="utf-8" />
                    <meta name="viewport" content="width=device-width, initial-scale=1" />
                    <title>%s OTP</title>
                  </head>
                  <body style="margin:0;padding:0;background:#f6f7fb;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;color:#0f172a;">
                    <div style="max-width:560px;margin:0 auto;padding:28px 16px;">
                      <div style="background:#ffffff;border:1px solid #e2e8f0;border-radius:14px;overflow:hidden;">
                        <div style="padding:18px 20px;background:linear-gradient(135deg,#0f172a,#1e293b);color:#ffffff;">
                          <div style="font-size:14px;letter-spacing:0.3px;opacity:0.95;">%s</div>
                          <div style="font-size:20px;font-weight:700;margin-top:2px;">One‑time password (OTP)</div>
                        </div>

                        <div style="padding:20px;">
                          <p style="margin:0 0 12px 0;font-size:14px;line-height:1.6;color:#334155;">
                            Use the code below to verify your identity. This code expires in <b>%s minutes</b>.
                          </p>

                          <div style="margin:16px 0;padding:16px;border:1px dashed #cbd5e1;border-radius:12px;background:#f8fafc;text-align:center;">
                            <div style="font-size:28px;font-weight:800;letter-spacing:6px;color:#0f172a;">%s</div>
                          </div>

                          <p style="margin:0 0 10px 0;font-size:13px;line-height:1.6;color:#475569;">
                            If you didn’t request this OTP, you can safely ignore this email.
                          </p>

                          <p style="margin:0;font-size:12px;line-height:1.6;color:#64748b;">
                            For your security, never share this code with anyone.
                          </p>
                        </div>

                        <div style="padding:14px 20px;background:#f8fafc;border-top:1px solid #e2e8f0;">
                          <div style="font-size:12px;color:#64748b;">© %s</div>
                        </div>
                      </div>
                    </div>
                  </body>
                </html>
                """.formatted(brand, brand, ttl, code, brand);
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}

