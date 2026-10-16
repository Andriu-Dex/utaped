import { withConfirmation } from '../confirmation'
import { openAction } from '../open-action'
import { test, expect } from '@playwright/test'
import { randomUUID } from 'node:crypto'
import { api, login, acceptanceBaseURL } from './sample-fixtures'

const date = (days: number) => new Date(Date.now() + days * 86400000).toISOString().slice(0,10)
test('review inbox paginates and filters on desktop and dark tablet', async ({page}) => {
  await login(page,'dev.reviewer')
  const items=Array.from({length:225},(_,i)=>({id:randomUUID(),reviewId:randomUUID(),documentId:randomUUID(),title:'Documento de aceptación '+String(i+1).padStart(3,'0'),groupName:'Comisión de aceptación',periodName:'Período de aceptación',stageLabel:'Revisión técnica',requiresSignature:false,status:i<5?'ACTIVE':'COMPLETED',roundNumber:1,documentType:i%2?'T2':'T1'}))
  await page.route('**/api/reviews/inbox?*',route=>{
    const params=new URL(route.request().url()).searchParams, pageNumber=Number(params.get('page')), size=Number(params.get('size')), query=params.get('query')||'', status=params.get('status'), type=params.get('documentType')
    const filtered=items.filter(t=>(!query||t.title.includes(query))&&(!status||t.status===status)&&(!type||t.documentType===type))
    return route.fulfill({json:{items:filtered.slice(pageNumber*size,(pageNumber+1)*size),total:filtered.length,page:pageNumber,size,metrics:{active:5,completed:220,returned:0}}})
  })
  await page.getByRole('button',{name:'Revisión documental',exact:true}).click();await expect(page.getByText('225 tareas encontradas',{exact:true})).toBeVisible();await expect(page.getByRole('row')).toHaveCount(21)
  await page.getByRole('button',{name:'Página siguiente',exact:true}).click();await expect(page.getByText('21–40 de 225',{exact:true})).toBeVisible();await expect(page.getByRole('row').filter({hasText:'Documento de aceptación 021'})).toBeVisible()
  await page.getByLabel('Estado de la tarea').selectOption('ACTIVE');await expect(page.getByText('5 tareas encontradas',{exact:true})).toBeVisible();await expect(page.getByRole('button',{name:'Página anterior',exact:true})).toBeDisabled()
  await page.getByLabel('Tipo de documento').selectOption('T2');await expect(page.getByText('2 tareas encontradas',{exact:true})).toBeVisible()
  await page.getByLabel('Buscar documento, ámbito o período').fill('sin coincidencias');await page.getByRole('button',{name:'Buscar',exact:true}).click();await expect(page.getByText('No hay tareas con estos filtros.',{exact:true})).toBeVisible()
  await page.getByRole('button',{name:'Limpiar filtros',exact:true}).click();await page.getByLabel('Tareas por página').selectOption('10');await expect(page.getByRole('row')).toHaveCount(11);await page.screenshot({path:'test-results/review-inbox-desktop.png',fullPage:true})
  await page.getByRole('button',{name:'Modo oscuro',exact:true}).click();await page.setViewportSize({width:820,height:1180});await expect.poll(()=>page.locator('.theme-toggle').evaluate(e=>getComputedStyle(e).backgroundColor)).toMatch(/^rgb\((23, 43, 62|29, 58, 83)\)$/);await page.screenshot({path:'test-results/review-inbox-tablet-dark.png',fullPage:true});expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBeTruthy()
})
test('career history and explicitly authorized coordinator recovery on desktop and tablet', async ({ browser }) => {
  const contexts = await Promise.all(Array.from({length:3}, () => browser.newContext({ baseURL: acceptanceBaseURL, viewport: { width: 1440, height: 1000 } })))
  const [admin, coordinator, member] = await Promise.all(contexts.map(c => c.newPage()))
  const suffix = randomUUID().slice(0,8), initial = 'E2E-only-initial-password-2026', next = 'E2E-only-reset-password-2026'
  try {
    await login(admin, 'dev.admin'); const actor = await login(coordinator, 'dev.principal')
    const user = await api(admin, '/admin/users', 'POST', { email: 'recovery.'+suffix+'@example.invalid', username: 'recovery.'+suffix, firstNames: 'Integrante', lastNames: 'Recuperación '+suffix, systemRole: 'USER', temporaryPassword: initial })
    await login(member, 'recovery.'+suffix, initial); await expect(member.getByRole('heading', {name:'Acceso temporal'})).toBeVisible()
    await member.getByLabel('Contraseña actual', {exact:true}).fill(initial); await member.getByLabel('Nueva contraseña', {exact:true}).fill('E2E-only-first-change-password-2026'); await member.getByLabel('Confirmar contraseña', {exact:true}).fill('E2E-only-first-change-password-2026'); await member.getByRole('button', {name:'Guardar contraseña',exact:true}).click(); await expect(member.getByRole('button',{name:'Documentos',exact:true})).toBeVisible()
    const career = await api(admin, '/admin/careers', 'POST', {code:'REC_'+suffix.toUpperCase(),name:'Carrera recuperación '+suffix})
    const group = await api(admin, '/admin/groups', 'POST', {name:'Ámbito recuperación '+suffix,groupType:'COMMISSION'})
    const period = await api(admin, '/admin/periods', 'POST', {name:'Período recuperación '+suffix,startsOn:date(-1),endsOn:date(30),preparationStartsOn:date(-1),preparationEndsOn:date(10),reviewStartsOn:date(-1),reviewEndsOn:date(30)})
    const scope = await api(admin, '/admin/organization/scopes', 'POST', {groupId:group.id,periodId:period.id,careerId:career.id})
    await api(admin, '/admin/organization/scopes/'+scope.id+'/members', 'PUT', {userId:actor.id,membershipRole:'COORDINATOR',positionLabel:'Coordinación inicial',active:true,rowVersion:0,passwordResetAuthorized:false})
    await api(admin, '/admin/organization/scopes/'+scope.id+'/members', 'PUT', {userId:user.id,membershipRole:'MEMBER',positionLabel:'Docente asignado',active:true,rowVersion:0})
    await api(admin, '/admin/organization/scopes/'+scope.id+'/principal', 'PUT', {userId:actor.id,rowVersion:0})
    await coordinator.reload(); await coordinator.getByRole('button',{name:'Mi organización',exact:true}).click(); await coordinator.getByLabel('Mi ámbito institucional').selectOption(scope.id); await expect(coordinator.getByText(/No hay cuentas que pueda restablecer/)).toBeVisible()
    await admin.reload(); await admin.getByRole('button',{name:'Carreras y ámbitos',exact:true}).click(); await admin.getByLabel('Ámbito institucional',{exact:true}).selectOption(scope.id)
    await openAction(admin, 'Asignar o editar integrante');await admin.getByLabel('Buscar integrante').fill(actor.username); await admin.getByLabel('Integrante del ámbito').selectOption(actor.id)
    await admin.getByLabel('Cargo o función').fill('Coordinación autorizada'); await admin.getByLabel('Autorizar recuperación de cuentas de este ámbito (solo coordinadores)').check(); await admin.getByRole('button',{name:'Guardar pertenencia',exact:true}).click(); await expect(admin.getByText('Configuración guardada.',{exact:true})).toBeVisible()
    await admin.getByText('Editar o desactivar una carrera',{exact:true}).click(); await admin.getByLabel('Carrera a editar').selectOption(career.id); await admin.getByLabel('Nombre actualizado').fill('Carrera actualizada '+suffix); await admin.getByRole('button',{name:'Guardar carrera',exact:true}).click(); await expect(admin.getByText('Carrera actualizada. Sus ámbitos e historial se conservan.',{exact:true})).toBeVisible()
    await coordinator.reload(); await coordinator.getByRole('button',{name:'Mi organización',exact:true}).click(); await coordinator.getByLabel('Mi ámbito institucional').selectOption(scope.id)
    await coordinator.getByText('Historial de cargos y responsables',{exact:true}).click(); await expect(coordinator.getByText('Coordinador · Coordinación inicial · Activo',{exact:true})).toBeVisible(); await expect(coordinator.getByText(/Coordinación autorizada · Activo/)).toBeVisible()
    await openAction(coordinator, 'Restablecer cuenta de integrante');await coordinator.getByLabel('Cuenta del integrante').selectOption(user.id); await coordinator.getByLabel('Contraseña temporal',{exact:true}).fill(next)
    await coordinator.evaluate(() => { (document.activeElement as HTMLElement)?.blur(); window.scrollTo({top:0,behavior:'instant'}) });; await coordinator.screenshot({path:'test-results/contextual-desktop.png',fullPage:true});  await withConfirmation(coordinator, true, async () => { await coordinator.getByRole('button',{name:'Restablecer acceso',exact:true}).click(); }); await expect(coordinator.getByText(/Acceso restablecido. Entregue/)).toBeVisible()
    expect((await member.request.get('/api/auth/me')).status()).toBe(401)
    await login(member,'recovery.'+suffix,next); await expect(member.getByRole('heading',{name:'Acceso temporal'})).toBeVisible()
    await member.getByLabel('Contraseña actual',{exact:true}).fill(next); await member.getByLabel('Nueva contraseña',{exact:true}).fill('E2E-only-final-password-2026'); await member.getByLabel('Confirmar contraseña',{exact:true}).fill('E2E-only-final-password-2026'); await member.getByRole('button',{name:'Guardar contraseña',exact:true}).click(); await expect(member.getByRole('button',{name:'Documentos',exact:true})).toBeVisible()
    const notice = (await api(member,'/notifications?size=100')).items.find((n:{kind:string})=>n.kind==='COORDINATOR_PASSWORD_RESET'); expect(notice.targetType).toBe('ACCOUNT');expect(notice.targetAvailable).toBe(true)
    await api(member,'/notifications/'+notice.id+'/open','POST')
    await coordinator.setViewportSize({width:1024,height:900}); await coordinator.getByRole('button',{name:'Modo oscuro',exact:true}).click(); await expect.poll(()=>coordinator.evaluate(()=>document.documentElement.dataset.theme)).toBe('dark'); await coordinator.evaluate(() => window.scrollTo({top:0,behavior:'instant'})); await coordinator.screenshot({path:'test-results/contextual-tablet-dark.png',fullPage:true}); expect(await coordinator.evaluate(()=>document.body.scrollWidth<=innerWidth)).toBe(true)
    await api(admin,'/admin/organization/scopes/'+scope.id+'/members','PUT',{userId:actor.id,membershipRole:'COORDINATOR',positionLabel:'Coordinación autorizada',active:true,rowVersion:2,passwordResetAuthorized:false})
    const csrf = await (await coordinator.request.get('/api/auth/csrf')).json(); expect((await coordinator.request.post('/api/organization/scopes/'+scope.id+'/accounts/'+user.id+'/temporary-password',{headers:{[csrf.headerName]:csrf.token},data:{rowVersion:3,temporaryPassword:'Blocked-only-password-2026'}})).status()).toBe(403)
  } finally { await Promise.all(contexts.map(c=>c.close())) }
})
