import com.sun.net.httpserver.*;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.*;

/** Community Reporting System back end. Database = Java ArrayLists (in memory, resets on restart). */
public class Main {
  static List<Map<String,String>> residents = new ArrayList<>(), reports = new ArrayList<>(), news = new ArrayList<>();
  static Map<String,String[]> sessions = new HashMap<>(); // token -> {role, id}
  static int rid = 1, pid = 1, nid = 1;
  static final String[] CATS = {"Road and Infrastructure","Garbage and Sanitation","Streetlights and Electricity","Water and Drainage","Noise and Disturbance","Safety and Security","Others"};
  static final List<String> STAT = List.of("Pending","In Progress","Resolved","Rejected");

  static Map<String,String> m(String... kv) { Map<String,String> o = new LinkedHashMap<>(); for (int i = 0; i < kv.length; i += 2) o.put(kv[i], kv[i+1]); return o; }
  static String esc(String s) { return s == null ? "" : s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r",""); }
  static String js(Map<String,String> o) { StringJoiner j = new StringJoiner(",","{","}"); for (var e : o.entrySet()) if (!e.getKey().equals("password")) j.add("\"" + e.getKey() + "\":\"" + esc(e.getValue()) + "\""); return j.toString(); }
  static String js(List<Map<String,String>> l) { StringJoiner j = new StringJoiner(",","[","]"); for (var o : l) j.add(js(o)); return j.toString(); }
  static String ok(String msg, String data) { return "{\"success\":true,\"message\":\"" + esc(msg) + "\",\"data\":" + data + "}"; }
  static String err(String msg) { return "{\"success\":false,\"message\":\"" + esc(msg) + "\"}"; }
  static Map<String,String> parse(String s) { Map<String,String> o = new HashMap<>(); Matcher k = Pattern.compile("\"(\\w+)\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(s); while (k.find()) o.put(k.group(1), k.group(2).replace("\\n","\n").replace("\\\"","\"").replace("\\\\","\\")); return o; }
  static Map<String,String> find(List<Map<String,String>> l, String id) { for (var o : l) if (o.get("id").equals(id)) return o; return null; }
  static String qp(String qs, String k) { for (String kv : qs.split("&")) { String[] a = kv.split("=",2); if (a[0].equals(k) && a.length > 1) return java.net.URLDecoder.decode(a[1], java.nio.charset.StandardCharsets.UTF_8); } return ""; }
  static String today() { return LocalDate.now().toString(); }
  static void send(HttpExchange x, int code, String body) throws IOException { byte[] b = body.getBytes("UTF-8"); x.getResponseHeaders().set("Content-Type","application/json; charset=utf-8"); x.sendResponseHeaders(code, b.length); x.getResponseBody().write(b); x.close(); }

  static String check(Map<String,String> b, boolean reg) {
    String n = b.getOrDefault("fullName","").trim(), a = b.getOrDefault("address","").trim(), c = b.getOrDefault("contactNumber","");
    if (n.length() < 2 || n.length() > 50) return "Full name must be 2 to 50 characters";
    if (a.isEmpty()) return "Address is required";
    if (!c.matches("\\d{10,11}")) return "Contact number must be 10 to 11 digits";
    if (reg) { String u = b.getOrDefault("username",""), p = b.getOrDefault("password","");
      if (u.length() < 4 || u.length() > 20) return "Username must be 4 to 20 characters";
      if (p.length() < 6) return "Password must be at least 6 characters"; }
    return null;
  }

  public static void main(String[] a) throws Exception {
    residents.add(m("id", ""+rid++, "fullName","Juan Dela Cruz", "address","Purok 1, Santo Tomas", "contactNumber","09171234567", "username","juan", "password","123456"));
    residents.add(m("id", ""+rid++, "fullName","Maria Santos", "address","Purok 2, Santo Tomas", "contactNumber","09181234567", "username","maria", "password","123456"));
    reports.add(m("id",""+pid++, "residentId","1", "category",CATS[2], "title","Broken streetlight", "description","The light near the chapel has been off for a week.", "location","Chapel Road", "status","Pending", "adminRemark","", "dateSubmitted",today(), "dateUpdated",today()));
    reports.add(m("id",""+pid++, "residentId","1", "category",CATS[1], "title","Uncollected garbage", "description","Garbage not collected since Monday.", "location","Purok 1 corner", "status","In Progress", "adminRemark","Collector scheduled", "dateSubmitted",today(), "dateUpdated",today()));
    reports.add(m("id",""+pid++, "residentId","2", "category",CATS[3], "title","Clogged drainage", "description","Water floods the street when it rains.", "location","Market Street", "status","Resolved", "adminRemark","Cleaned", "dateSubmitted",today(), "dateUpdated",today()));
    news.add(m("id",""+nid++, "title","Barangay Clean-Up Drive", "content","Join us this Saturday, 7 AM at the barangay hall.", "datePosted",today()));
    news.add(m("id",""+nid++, "title","Water Interruption", "content","Expect low pressure on Thursday from 9 AM to 3 PM.", "datePosted",today()));

    HttpServer s = HttpServer.create(new java.net.InetSocketAddress(8080), 0);
    s.createContext("/api", x -> { try { api(x); } catch (Exception e) { e.printStackTrace(); try { send(x, 500, err("Server error")); } catch (IOException i) {} } });
    s.createContext("/", Main::file);
    s.start();
    System.out.println("Running at http://localhost:8080   (admin / admin123, resident: juan / 123456)");
  }

  static void file(HttpExchange x) throws IOException {
    String p = x.getRequestURI().getPath(); if (p.equals("/")) p = "/index.html";
    Path f = Paths.get("frontend", p).normalize();
    if (!f.startsWith("frontend") || !Files.isRegularFile(f)) { x.sendResponseHeaders(404, -1); x.close(); return; }
    String n = f.toString(), t = n.endsWith(".html") ? "text/html" : n.endsWith(".css") ? "text/css" : n.endsWith(".js") ? "application/javascript" : "application/octet-stream";
    byte[] b = Files.readAllBytes(f);
    x.getResponseHeaders().set("Content-Type", t + "; charset=utf-8"); x.sendResponseHeaders(200, b.length); x.getResponseBody().write(b); x.close();
  }

  static void api(HttpExchange x) throws IOException {
    String p = x.getRequestURI().getPath().substring(4), me = x.getRequestMethod();
    String qs = x.getRequestURI().getQuery() == null ? "" : x.getRequestURI().getQuery();
    Map<String,String> b = parse(new String(x.getRequestBody().readAllBytes(), "UTF-8"));
    String tk = x.getRequestHeaders().getFirst("Authorization"); String[] s = sessions.get(tk);
    String role = s == null ? "" : s[0], uid = s == null ? "" : s[1];
    String[] q = p.split("/"); String e;

    if (p.equals("/register") && me.equals("POST")) {
      e = check(b, true);
      if (e == null) for (var r : residents) if (r.get("username").equalsIgnoreCase(b.get("username"))) e = "Username already exists";
      if (e != null) { send(x, 400, err(e)); return; }
      residents.add(m("id",""+rid++, "fullName",b.get("fullName").trim(), "address",b.get("address").trim(), "contactNumber",b.get("contactNumber"), "username",b.get("username"), "password",b.get("password")));
      send(x, 200, ok("Registered! You can now log in.", "{}")); return;
    }
    if (p.equals("/login") && me.equals("POST")) {
      String un = b.getOrDefault("username",""), pw = b.getOrDefault("password",""), r = null, id = null, name = null;
      if (un.equals("admin") && pw.equals("admin123")) { r = "admin"; id = "0"; name = "Admin"; }
      else for (var o : residents) if (o.get("username").equals(un) && o.get("password").equals(pw)) { r = "resident"; id = o.get("id"); name = o.get("fullName"); }
      if (r == null) { send(x, 401, err("Invalid username or password")); return; }
      String t = UUID.randomUUID().toString(); sessions.put(t, new String[]{r, id});
      send(x, 200, ok("Welcome", "{\"token\":\"" + t + "\",\"role\":\"" + r + "\",\"name\":\"" + esc(name) + "\"}")); return;
    }
    if (p.equals("/categories")) { StringJoiner j = new StringJoiner(",","[","]"); for (String c : CATS) j.add("\"" + c + "\""); send(x, 200, ok("", j.toString())); return; }
    if (p.equals("/logout")) { sessions.remove(tk); send(x, 200, ok("Logged out", "{}")); return; }
    if (s == null) { send(x, 401, err("Please log in")); return; }

    if (p.equals("/announcements") && me.equals("GET")) { List<Map<String,String>> l = new ArrayList<>(news); Collections.reverse(l); send(x, 200, ok("", js(l))); return; }

    // ---------- ADMIN ----------
    if (q.length > 1 && q[1].equals("admin")) {
      if (!role.equals("admin")) { send(x, 403, err("Admins only")); return; }
      String what = q.length > 2 ? q[2] : "", id = q.length > 3 ? q[3] : "";
      if (what.equals("stats")) {
        int[] c = new int[4]; for (var r : reports) c[STAT.indexOf(r.get("status"))]++;
        send(x, 200, ok("", "{\"total\":\"" + reports.size() + "\",\"pending\":\"" + c[0] + "\",\"inProgress\":\"" + c[1] + "\",\"resolved\":\"" + c[2] + "\",\"rejected\":\"" + c[3] + "\",\"residents\":\"" + residents.size() + "\"}")); return;
      }
      if (what.equals("reports")) {
        if (me.equals("GET")) {
          String fs = qp(qs,"status"), fc = qp(qs,"category"); List<Map<String,String>> l = new ArrayList<>();
          for (var r : reports) { if (!fs.isEmpty() && !r.get("status").equals(fs)) continue; if (!fc.isEmpty() && !r.get("category").equals(fc)) continue;
            Map<String,String> c = new LinkedHashMap<>(r); var rs = find(residents, r.get("residentId")); c.put("residentName", rs == null ? "(deleted)" : rs.get("fullName")); l.add(c); }
          Collections.reverse(l); send(x, 200, ok("", js(l))); return;
        }
        var r = find(reports, id); if (r == null) { send(x, 404, err("Report not found")); return; }
        if (me.equals("PUT") && q.length > 4 && q[4].equals("status")) {
          if (!STAT.contains(b.get("status"))) { send(x, 400, err("Invalid status")); return; }
          r.put("status", b.get("status")); r.put("adminRemark", b.getOrDefault("adminRemark","")); r.put("dateUpdated", today());
          send(x, 200, ok("Status updated", js(r))); return;
        }
        if (me.equals("DELETE")) { reports.remove(r); send(x, 200, ok("Report deleted", "{}")); return; }
      }
      if (what.equals("announcements")) {
        if (me.equals("POST")) {
          if (b.getOrDefault("title","").trim().isEmpty() || b.getOrDefault("content","").trim().isEmpty()) { send(x, 400, err("Title and content are required")); return; }
          news.add(m("id",""+nid++, "title",b.get("title").trim(), "content",b.get("content").trim(), "datePosted",today())); send(x, 200, ok("Announcement posted", "{}")); return;
        }
        var a = find(news, id); if (a == null) { send(x, 404, err("Not found")); return; }
        if (me.equals("PUT")) {
          if (b.getOrDefault("title","").trim().isEmpty() || b.getOrDefault("content","").trim().isEmpty()) { send(x, 400, err("Title and content are required")); return; }
          a.put("title", b.get("title").trim()); a.put("content", b.get("content").trim()); send(x, 200, ok("Announcement updated", "{}")); return;
        }
        if (me.equals("DELETE")) { news.remove(a); send(x, 200, ok("Announcement deleted", "{}")); return; }
      }
      if (what.equals("residents")) {
        if (me.equals("GET")) { send(x, 200, ok("", js(residents))); return; }
        var r = find(residents, id); if (r == null) { send(x, 404, err("Resident not found")); return; }
        if (me.equals("PUT")) {
          e = check(b, false); if (e != null) { send(x, 400, err(e)); return; }
          r.put("fullName", b.get("fullName").trim()); r.put("address", b.get("address").trim()); r.put("contactNumber", b.get("contactNumber")); send(x, 200, ok("Resident updated", "{}")); return;
        }
        if (me.equals("DELETE")) { residents.remove(r); reports.removeIf(o -> o.get("residentId").equals(id)); send(x, 200, ok("Resident and their reports deleted", "{}")); return; }
      }
      send(x, 404, err("Not found")); return;
    }

    // ---------- RESIDENT ----------
    if (!role.equals("resident")) { send(x, 403, err("Residents only")); return; }
    if (p.equals("/reports") && me.equals("POST")) {
      String t = b.getOrDefault("title","").trim(), d = b.getOrDefault("description","").trim(), l = b.getOrDefault("location","").trim();
      e = t.isEmpty() || t.length() > 100 ? "Title is required (max 100 characters)" : d.isEmpty() || d.length() > 500 ? "Description is required (max 500 characters)" : l.isEmpty() ? "Location is required" : !Arrays.asList(CATS).contains(b.get("category")) ? "Choose a valid category" : null;
      if (e != null) { send(x, 400, err(e)); return; }
      reports.add(m("id",""+pid++, "residentId",uid, "category",b.get("category"), "title",t, "description",d, "location",l, "status","Pending", "adminRemark","", "dateSubmitted",today(), "dateUpdated",today()));
      send(x, 200, ok("Report submitted", "{}")); return;
    }
    if (p.equals("/reports/mine")) { List<Map<String,String>> l = new ArrayList<>(); for (var r : reports) if (r.get("residentId").equals(uid)) l.add(r); Collections.reverse(l); send(x, 200, ok("", js(l))); return; }
    if (p.equals("/profile")) {
      var r = find(residents, uid);
      if (me.equals("GET")) { send(x, 200, ok("", js(r))); return; }
      if (me.equals("PUT")) { e = check(b, false); if (e != null) { send(x, 400, err(e)); return; }
        r.put("fullName", b.get("fullName").trim()); r.put("address", b.get("address").trim()); r.put("contactNumber", b.get("contactNumber")); send(x, 200, ok("Profile updated", js(r))); return; }
    }
    send(x, 404, err("Not found"));
  }
}
