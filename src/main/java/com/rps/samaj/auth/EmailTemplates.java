package com.rps.samaj.auth;

public final class EmailTemplates {

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

    public static String adminInviteHtml(String toEmail, String inviteUrl, String serviceNames, long ttlHours) {
        String email = esc(toEmail != null ? toEmail : "");
        String url = esc(inviteUrl != null ? inviteUrl : "#");
        String services = esc(serviceNames != null ? serviceNames : "");
        String ttl = String.valueOf(ttlHours > 0 ? ttlHours : 48);

        return """
                <!doctype html>
                <html lang="en">
                  <head>
                    <meta charset="utf-8" />
                    <meta name="viewport" content="width=device-width, initial-scale=1" />
                    <title>Admin Invitation — Samaj</title>
                  </head>
                  <body style="margin:0;padding:0;background:#f1f5f9;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;color:#0f172a;">
                    <div style="max-width:580px;margin:0 auto;padding:32px 16px;">
                      <div style="background:#ffffff;border:1px solid #e2e8f0;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.06);">

                        <!-- Header -->
                        <div style="padding:24px 28px;background:linear-gradient(135deg,#0f172a 0%%,#1e3a5f 100%%);color:#ffffff;">
                          <div style="display:flex;align-items:center;gap:10px;margin-bottom:6px;">
                            <div style="width:32px;height:32px;background:rgba(255,255,255,0.12);border-radius:8px;display:flex;align-items:center;justify-content:center;font-size:16px;font-weight:700;">स</div>
                            <span style="font-size:15px;font-weight:600;opacity:0.9;">Samaj Admin</span>
                          </div>
                          <h1 style="margin:8px 0 4px;font-size:22px;font-weight:700;line-height:1.3;">You've been invited as an Admin</h1>
                          <p style="margin:0;font-size:13px;opacity:0.7;">Manage specific services in the Samaj platform</p>
                        </div>

                        <!-- Body -->
                        <div style="padding:24px 28px;">
                          <p style="margin:0 0 16px;font-size:14px;line-height:1.7;color:#334155;">
                            Hello, <b>%s</b> has been designated as a sub-admin on Samaj. You can access and manage the services listed below.
                          </p>

                          <!-- Services -->
                          <div style="background:#f8fafc;border:1px solid #e2e8f0;border-radius:10px;padding:14px 16px;margin-bottom:20px;">
                            <p style="margin:0 0 6px;font-size:11px;font-weight:700;text-transform:uppercase;letter-spacing:0.8px;color:#64748b;">Assigned Services</p>
                            <p style="margin:0;font-size:14px;font-weight:600;color:#0f172a;">%s</p>
                          </div>

                          <!-- CTA -->
                          <div style="text-align:center;margin:24px 0;">
                            <a href="%s" style="display:inline-block;background:linear-gradient(135deg,#1d4ed8,#2563eb);color:#ffffff;text-decoration:none;font-size:15px;font-weight:700;padding:14px 36px;border-radius:10px;letter-spacing:0.2px;">
                              Accept Invitation &amp; Set Up Account
                            </a>
                          </div>

                          <p style="margin:16px 0 8px;font-size:12px;line-height:1.6;color:#64748b;">
                            If the button above doesn't work, paste this URL into your browser:
                          </p>
                          <p style="margin:0 0 16px;font-size:11px;color:#94a3b8;word-break:break-all;">%s</p>

                          <div style="background:#fefce8;border:1px solid #fde047;border-radius:8px;padding:10px 14px;">
                            <p style="margin:0;font-size:12px;color:#854d0e;">
                              ⚠ This invitation expires in <b>%s hours</b>. Do not share this link with anyone.
                            </p>
                          </div>
                        </div>

                        <!-- Footer -->
                        <div style="padding:14px 28px;background:#f8fafc;border-top:1px solid #e2e8f0;">
                          <p style="margin:0;font-size:11px;color:#94a3b8;">© Samaj &nbsp;·&nbsp; If you weren't expecting this, please ignore this email.</p>
                        </div>
                      </div>
                    </div>
                  </body>
                </html>
                """.formatted(email, services, url, url, ttl);
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

