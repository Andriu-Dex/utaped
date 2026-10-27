import { test, expect } from '@playwright/test'
import { randomUUID } from 'node:crypto'
import { api, login, acceptanceBaseURL } from './sample-fixtures'
import { openAction } from '../open-action'
import { withConfirmation } from '../confirmation'

const date = (days: number) => new Date(Date.now() + days * 86400000).toISOString().slice(0, 10)
test('versioned structures, private discarded drafts and verified custody in desktop and tablet', async ({ browser }) => {
  const contexts = await Promise.all([browser.newContext({baseURL:acceptanceBaseURL}), browser.newContext({baseURL:acceptanceBaseURL})])
  const [admin, author] = await Promise.all(contexts.map(c => c.newPage()))
  let original: { revision: number; sections: unknown[] } | undefined
  try {
    await login(admin,'dev.admin');const identity=await login(author,'dev.principal'), suffix=randomUUID().slice(0,8)
    original=await api(admin,'/admin/document-structures/T1')
    const group=await api(admin,'/admin/groups','POST',{name:'Comisión documental '+suffix,groupType:'COMMISSION'}), period=await api(admin,'/admin/periods','POST',{name:'Período documental '+suffix,startsOn:date(-2),endsOn:date(60),preparationStartsOn:date(-2),reviewStartsOn:date(-2),reviewEndsOn:date(60)})
    const scope=await api(admin,'/admin/organization/scopes','POST',{groupId:group.id,periodId:period.id,careerId:null})
    await api(admin,'/admin/organization/scopes/'+scope.id+'/members','PUT',{userId:identity.id,membershipRole:'MEMBER',positionLabel:'Responsable',active:true,rowVersion:0});await api(admin,'/admin/organization/scopes/'+scope.id+'/principal','PUT',{userId:identity.id,rowVersion:0})
    const create=()=>api(author,'/work-plans','POST',{scopeId:scope.id,groupId:group.id,periodId:period.id,requestKey:randomUUID()})
    const old=await create(), oldStructure=await api(author,'/work-plans/'+old.id+'/structure')
    await api(author,'/work-plans/'+old.id+'/archive','POST',{rowVersion:old.rowVersion,reason:'Descartar borrador antes de revisar la nueva estructura.'})
    expect((await author.request.get('/api/work-plans/'+old.id)).status()).toBe(404)
    expect((await api(admin,'/work-plans/'+old.id)).editable).toBe(false)
    await admin.getByRole('button',{name:'Plantillas',exact:true}).click()
    const structures=admin.getByRole('region',{name:'Estructura de documentos',exact:true})
    await expect(structures.getByRole('button',{name:'Agregar sección',exact:true})).toHaveCount(0)
    await openAction(structures,'Editar estructura');await structures.getByRole('button',{name:'Agregar sección',exact:true}).click();await structures.getByLabel('Título de la sección',{exact:true}).fill('Seguimiento del período');await structures.getByLabel('Obligatoria para preparar el documento',{exact:true}).check();await structures.getByLabel('Texto inicial',{exact:true}).fill('Contenido inicial de seguimiento.')
    await withConfirmation(admin,false,async()=>{await structures.getByLabel('Formato documental',{exact:true}).selectOption('T3')});await expect(structures.getByLabel('Título de la sección',{exact:true})).toHaveValue('Seguimiento del período')
    await admin.screenshot({path:'test-results/structure-administration-desktop.png',fullPage:true});await structures.getByRole('button',{name:'Publicar estructura',exact:true}).click();await expect(admin.getByText('Estructura publicada. Se aplicará solo a documentos nuevos.',{exact:true})).toBeVisible()
    expect((await api(admin,'/work-plans/'+old.id+'/structure')).structure).toEqual(oldStructure.structure)
    const current=await create();await author.reload();await author.getByRole('button',{name:'Documentos',exact:true}).click();await author.getByRole('button',{name:'Editar '+current.title,exact:false}).click();await author.getByRole('button',{name:/5\s*Revisión/}).click()
    const extra=author.getByRole('region',{name:'Secciones adicionales',exact:true});await expect(extra.getByRole('textbox')).toHaveCount(0);await openAction(extra,'Editar secciones adicionales');await expect(extra.getByLabel('Seguimiento del período',{exact:true})).toHaveValue('Contenido inicial de seguimiento.');await extra.getByLabel('Seguimiento del período',{exact:true}).fill('Seguimiento ajustado por el responsable.')
    await withConfirmation(author,false,async()=>{await extra.getByRole('button',{name:'Cancelar: Editar secciones adicionales',exact:true}).click()});await expect(extra.getByLabel('Seguimiento del período',{exact:true})).toHaveValue('Seguimiento ajustado por el responsable.');await extra.getByRole('button',{name:'Guardar secciones',exact:true}).click();await expect.poll(async()=>Object.values((await api(author,'/work-plans/'+current.id+'/structure')).structure.contents)).toContain('Seguimiento ajustado por el responsable.')
    await admin.reload();await admin.getByRole('button',{name:'Períodos académicos',exact:true}).click();await admin.getByLabel('Período a administrar').selectOption(period.id);await openAction(admin,'Preparar cierre del período');await admin.getByLabel('Motivo del cierre').fill('Custodia de documentos de la prueba aislada.');await withConfirmation(admin,true,async()=>{await admin.getByRole('button',{name:'Cerrar período',exact:true}).click()});await expect(admin.getByText('Cerrado · Solo lectura',{exact:true})).toBeVisible()
    const download=admin.waitForEvent('download');await admin.getByRole('button',{name:'Descargar copia de custodia',exact:true}).click();const zip=await download, stream=await zip.createReadStream(),chunks:Buffer[]=[];for await(const chunk of stream!)chunks.push(Buffer.from(chunk));const bytes=Buffer.concat(chunks);expect(bytes.subarray(0,2).toString()).toBe('PK')
    const csrf=await api(admin,'/auth/csrf');const oversizedForOrdinaryUploads=await admin.request.post('/api/admin/periods/'+period.id+'/custody-verification',{headers:{[csrf.headerName]:csrf.token,'Content-Type':'application/octet-stream'},data:Buffer.alloc(13*1024*1024)});expect(oversizedForOrdinaryUploads.status()).toBe(409)
    await openAction(admin,'Verificar copia de custodia');await admin.getByLabel('ZIP de custodia descargado',{exact:true}).setInputFiles({name:'custodia.zip',mimeType:'application/zip',buffer:Buffer.from('invalid')});await admin.getByRole('button',{name:'Verificar ZIP',exact:true}).click();await expect(admin.getByText('La copia no coincide con el ZIP original. No se habilitó el borrado.',{exact:true})).toBeVisible()
    await admin.getByLabel('ZIP de custodia descargado',{exact:true}).setInputFiles({name:'custodia.zip',mimeType:'application/zip',buffer:bytes});await admin.getByRole('button',{name:'Verificar ZIP',exact:true}).click();await expect(admin.getByText('Copia local verificada por SHA-256. Conserve el ZIP fuera del servidor.',{exact:true})).toBeVisible();expect((await api(admin,'/admin/periods/'+period.id+'/control')).custody_verified_at).toBeTruthy();await expect(admin.getByRole('button',{name:'Eliminar documentación del período',exact:true})).toHaveCount(0)
    await admin.setViewportSize({width:820,height:1180});await admin.getByRole('button',{name:'Modo oscuro',exact:true}).click();await admin.screenshot({path:'test-results/custody-verification-tablet-dark.png',fullPage:true});expect(await admin.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBeTruthy();await zip.delete()
  } finally {
    if(original) { const state=await api(admin,'/admin/document-structures/T1');await api(admin,'/admin/document-structures/T1','PUT',{revision:state.revision,sections:original.sections}) }
    for(const context of contexts)await context.close()
  }
})
