const $=s=>document.querySelector(s),fd=f=>Object.fromEntries(new FormData(f));
const S=()=>JSON.parse(localStorage.getItem('u')||'null');
const esc=s=>String(s??'').replace(/[&<>"]/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}[c]));
const badge=s=>`<span class="b ${s.replace(' ','')}">${s}</span>`;
async function api(m,u,b){const r=await fetch('/api'+u,{method:m,headers:{'Content-Type':'application/json',Authorization:(S()||{}).token||''},body:b?JSON.stringify(b):undefined});
  if(r.status==401&&S()){localStorage.removeItem('u');location='login.html'}return r.json()}
function msg(t,ok){const e=$('#msg');if(e){e.className='msg '+(ok?'ok':'bad');e.textContent=t}}
function logout(){api('POST','/logout');localStorage.removeItem('u');location='login.html'}
const news=n=>n.map(a=>`<div class="card"><h3>${esc(a.title)}</h3><small>${a.datePosted}</small><p>${esc(a.content)}</p></div>`).join('')||'<p>No announcements yet.</p>';

const u=S(),pg=document.body.dataset.page,need=document.body.dataset.role;
if(need&&(!u||u.role!=need))location='login.html';
const L={resident:[['resident-dashboard.html','Dashboard'],['submit-report.html','Submit Report'],['my-reports.html','My Reports'],['announcements.html','Announcements']],
 admin:[['admin-dashboard.html','Dashboard'],['manage-reports.html','Manage Reports']],
 pub:[['index.html','Home'],['about.html','About Us'],['login.html','Login'],['register.html','Register']]};
$('#nav').innerHTML='<b>📣 Community Reporting</b>'+L[u?u.role:'pub'].map(l=>`<a href="${l[0]}">${l[1]}</a>`).join('')+(u?`<a href="#" onclick="logout()">Logout (${esc(u.name)})</a>`:'');

const P={
login(){$('form').onsubmit=async e=>{e.preventDefault();const r=await api('POST','/login',fd(e.target));if(!r.success)return msg(r.message);
  localStorage.setItem('u',JSON.stringify(r.data));location=r.data.role=='admin'?'admin-dashboard.html':'resident-dashboard.html'}},
register(){$('form').onsubmit=async e=>{e.preventDefault();const r=await api('POST','/register',fd(e.target));msg(r.message,r.success);
  if(r.success){e.target.reset();setTimeout(()=>location='login.html',1200)}}},
async 'resident-dashboard'(){
  $('#hi').textContent='Welcome, '+u.name+'!';
  const r=(await api('GET','/reports/mine')).data||[];
  $('#stats').innerHTML=['Pending','In Progress','Resolved'].map(s=>`<div class="card"><h2>${r.filter(x=>x.status==s).length}</h2>${s}</div>`).join('');
  $('#news').innerHTML=news(((await api('GET','/announcements')).data||[]).slice(0,3));
  const p=(await api('GET','/profile')).data,f=$('#pf');['fullName','address','contactNumber'].forEach(k=>f[k].value=p[k]);
  f.onsubmit=async e=>{e.preventDefault();const r=await api('PUT','/profile',fd(f));msg(r.message,r.success);
    if(r.success){u.name=f.fullName.value;localStorage.setItem('u',JSON.stringify(u));$('#hi').textContent='Welcome, '+u.name+'!'}}},
async 'submit-report'(){
  $('#cat').innerHTML=(await api('GET','/categories')).data.map(c=>`<option>${c}</option>`).join('');
  $('form').onsubmit=async e=>{e.preventDefault();const r=await api('POST','/reports',fd(e.target));msg(r.message,r.success);if(r.success)e.target.reset()}},
async 'my-reports'(){
  const r=(await api('GET','/reports/mine')).data;
  $('#rows').innerHTML=r.map(x=>`<tr><td>${x.id}</td><td><b>${esc(x.title)}</b><br><small>${esc(x.description)}</small></td><td>${esc(x.category)}</td><td>${esc(x.location)}</td><td>${badge(x.status)}</td><td>${esc(x.adminRemark)||'-'}</td><td>${x.dateSubmitted}</td></tr>`).join('')||'<tr><td colspan=7>No reports yet.</td></tr>'},
async announcements(){$('#news').innerHTML=news((await api('GET','/announcements')).data)},
async 'admin-dashboard'(){
  const s=(await api('GET','/admin/stats')).data;
  $('#stats').innerHTML=[['Total Reports','total'],['Pending','pending'],['In Progress','inProgress'],['Resolved','resolved'],['Residents','residents']].map(k=>`<div class="card"><h2>${s[k[1]]}</h2>${k[0]}</div>`).join('');
  window.tab=n=>['tn','tr'].forEach(i=>$('#'+i).hidden=i!=n);tab('tn');
  window.loadN=async()=>{window.N=(await api('GET','/announcements')).data;
    $('#news').innerHTML=N.map(a=>`<div class="card"><h3>${esc(a.title)}</h3><small>${a.datePosted}</small><p>${esc(a.content)}</p><button onclick="editN(${a.id})">Edit</button> <button class="del" onclick="delN(${a.id})">Delete</button></div>`).join('')||'<p>None yet.</p>'};
  window.editN=async id=>{const a=N.find(x=>x.id==id),t=prompt('Title',a.title);if(!t)return;const c=prompt('Content',a.content);if(!c)return;
    const r=await api('PUT','/admin/announcements/'+id,{title:t,content:c});msg(r.message,r.success);loadN()};
  window.delN=async id=>{if(confirm('Delete this announcement?')){await api('DELETE','/admin/announcements/'+id);loadN()}};
  $('#nf').onsubmit=async e=>{e.preventDefault();const r=await api('POST','/admin/announcements',fd(e.target));msg(r.message,r.success);if(r.success)e.target.reset();loadN()};
  window.loadR=async()=>{window.R=(await api('GET','/admin/residents')).data;
    $('#rrows').innerHTML=R.map(r=>`<tr><td>${r.id}</td><td>${esc(r.fullName)}</td><td>${esc(r.address)}</td><td>${esc(r.contactNumber)}</td><td>${esc(r.username)}</td><td><button onclick="editR(${r.id})">Edit</button> <button class="del" onclick="delR(${r.id})">Delete</button></td></tr>`).join('')||'<tr><td colspan=6>No residents.</td></tr>'};
  window.editR=async id=>{const r=R.find(x=>x.id==id),n=prompt('Full name',r.fullName);if(!n)return;const a=prompt('Address',r.address);if(!a)return;const c=prompt('Contact number',r.contactNumber);if(!c)return;
    const x=await api('PUT','/admin/residents/'+id,{fullName:n,address:a,contactNumber:c});msg(x.message,x.success);loadR()};
  window.delR=async id=>{if(confirm('Delete this resident and all their reports?')){const x=await api('DELETE','/admin/residents/'+id);msg(x.message,x.success);loadR()}};
  loadN();loadR()},
async 'manage-reports'(){
  $('#fc').innerHTML='<option value="">All categories</option>'+(await api('GET','/categories')).data.map(c=>`<option>${c}</option>`).join('');
  window.load=async()=>{const r=await api('GET','/admin/reports?status='+encodeURIComponent($('#fs').value)+'&category='+encodeURIComponent($('#fc').value));
    $('#rows').innerHTML=r.data.map(x=>`<tr><td>${x.id}</td><td>${esc(x.residentName)}</td><td>${esc(x.category)}</td><td><b>${esc(x.title)}</b><br><small>${esc(x.description)}<br>📍 ${esc(x.location)} · ${x.dateSubmitted}</small></td>
      <td><select id="s${x.id}">${['Pending','In Progress','Resolved','Rejected'].map(s=>`<option ${s==x.status?'selected':''}>${s}</option>`).join('')}</select><input id="r${x.id}" placeholder="Admin remark" value="${esc(x.adminRemark)}"></td>
      <td><button onclick="upd(${x.id})">Save</button> <button class="del" onclick="delRep(${x.id})">Delete</button></td></tr>`).join('')||'<tr><td colspan=6>No reports found.</td></tr>'};
  window.upd=async id=>{const r=await api('PUT','/admin/reports/'+id+'/status',{status:$('#s'+id).value,adminRemark:$('#r'+id).value});msg(r.message,r.success)};
  window.delRep=async id=>{if(confirm('Delete this report?')){const r=await api('DELETE','/admin/reports/'+id);msg(r.message,r.success);load()}};
  load()}
};
P[pg]&&P[pg]();
