import { randomUUID } from 'node:crypto'
import { test, expect } from '@playwright/test'
import { api, login } from './sample-fixtures'
import { withConfirmation } from '../confirmation'

test.describe('Functional closeout: Review Reassignment and Multi-Career Copy', () => {
  test('1. task permissions control the reassignment action; completed and unauthorized stages omit it', async ({ page }) => {
    const actor = await login(page, 'dev.coordinator')
    const reviewId = randomUUID(), taskId1 = randomUUID(), taskId2 = randomUUID(), taskId3 = randomUUID()
    const docTitle = 'Plan de Trabajo: Tutoría de Acompañamiento'

    await page.route('**/api/reviews/inbox?*', route => route.fulfill({
      json: {
        items: [{
          id: taskId1,
          reviewId,
          documentId: randomUUID(),
          title: docTitle,
          groupName: 'Comisión de carrera',
          periodName: 'Julio–Diciembre 2026',
          stageLabel: 'Revisión técnica',
          action: 'REVIEW',
          requiresSignature: false,
          status: 'ACTIVE',
          roundNumber: 1
        }],
        total: 1,
        page: 0,
        size: 20,
        metrics: { active: 1, completed: 0, returned: 0 }
      }
    }))

    await page.route('**/api/reviews/' + reviewId, route => route.fulfill({
      json: {
        review: { id: reviewId, title: docTitle, status: 'IN_REVIEW', round_number: 1 },
        tasks: [
          {
            id: taskId1,
            actor_id: actor.id,
            actor_name: actor.displayName,
            stage_label: 'Revisión técnica',
            stage_action: 'REVIEW',
            requires_signature: false,
            status: 'ACTIVE',
            row_version: 1,
            can_reassign: true,
            slot_actor_id: null,
            reassignments: []
          },
          {
            id: taskId2,
            actor_id: randomUUID(),
            actor_name: 'Dr. Fernando Medina',
            stage_label: 'Revisión metodológica',
            stage_action: 'REVIEW',
            requires_signature: false,
            status: 'COMPLETED',
            row_version: 1,
            can_reassign: false,
            slot_actor_id: null,
            reassignments: []
          },
          {
            id: taskId3,
            actor_id: randomUUID(),
            actor_name: 'Mgs. Patricia Salazar',
            stage_label: 'Aprobación de decanato',
            stage_action: 'APPROVE',
            requires_signature: true,
            status: 'PENDING',
            row_version: 1,
            can_reassign: false,
            slot_actor_id: null,
            reassignments: []
          }
        ],
        observations: [],
        pageCount: 1,
        pdfHash: 'c'.repeat(64),
        canAct: true
      }
    }))

    await page.route('**/api/reviews/' + reviewId + '/pages/*', route => route.fulfill({
      contentType: 'image/png',
      body: Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a9XcAAAAASUVORK5CYII=', 'base64')
    }))

    await page.getByRole('button', { name: 'Revisión documental', exact: true }).click()
    await page.getByRole('button', { name: 'Revisar ' + docTitle + ' · Ronda 1', exact: true }).click()

    // Task 1 with can_reassign=true MUST show "Sustituir participante"
    const reassignBtn = page.getByRole('button', { name: 'Sustituir participante de Revisión técnica', exact: true })
    await expect(reassignBtn).toBeVisible()

    // Task 2 (COMPLETED) and Task 3 (can_reassign=false) MUST NOT show reassignment
    await expect(page.getByRole('button', { name: /Sustituir participante de Revisión metodológica/i })).toHaveCount(0)
    await expect(page.getByRole('button', { name: /Sustituir participante de Aprobación de decanato/i })).toHaveCount(0)
  })

  test('2. unavailable stages and candidate combo; required reason and conflict retain data', async ({ page }) => {
    const actor = await login(page, 'dev.coordinator')
    const reviewId = randomUUID(), taskId = randomUUID()
    const docTitle = 'Plan de Auditoría Docente'
    const candidate1 = { id: randomUUID(), name: 'Dra. María Elena Quispe' }
    const candidate2 = { id: randomUUID(), name: 'Ing. Roberto Silva' }

    await page.route('**/api/reviews/inbox?*', route => route.fulfill({
      json: {
        items: [{
          id: taskId,
          reviewId,
          documentId: randomUUID(),
          title: docTitle,
          groupName: 'Comisión de carrera',
          periodName: 'Julio–Diciembre 2026',
          stageLabel: 'Revisión técnica',
          status: 'ACTIVE',
          roundNumber: 1
        }],
        total: 1, page: 0, size: 20, metrics: { active: 1, completed: 0, returned: 0 }
      }
    }))

    await page.route('**/api/reviews/' + reviewId, route => route.fulfill({
      json: {
        review: { id: reviewId, title: docTitle, status: 'IN_REVIEW', round_number: 1 },
        tasks: [{
          id: taskId,
          actor_id: actor.id,
          actor_name: actor.displayName,
          stage_label: 'Revisión técnica',
          status: 'ACTIVE',
          row_version: 1,
          can_reassign: true
        }],
        observations: [],
        pageCount: 1,
        pdfHash: 'd'.repeat(64),
        canAct: true
      }
    }))

    await page.route('**/api/reviews/' + reviewId + '/pages/*', route => route.fulfill({
      contentType: 'image/png',
      body: Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a9XcAAAAASUVORK5CYII=', 'base64')
    }))

    // Scenario 2a: Empty candidates
    let mockCandidates = {
      rowVersion: 1,
      actorId: actor.id,
      actorName: actor.displayName,
      canReassign: false,
      candidates: [] as typeof candidate1[],
      history: []
    }
    await page.route('**/api/reviews/tasks/' + taskId + '/reassignment', route => {
      if (route.request().method() === 'GET') {
        return route.fulfill({ json: mockCandidates })
      }
      return route.continue()
    })

    await page.getByRole('button', { name: 'Revisión documental', exact: true }).click()
    await page.getByRole('button', { name: 'Revisar ' + docTitle + ' · Ronda 1', exact: true }).click()
    await page.getByRole('button', { name: 'Sustituir participante de Revisión técnica', exact: true }).click()

    const modal = page.locator('.task-reassignment-dialog')
    await expect(modal).toBeVisible()
    await expect(modal.getByText('Esta etapa no admite sustitución en este momento.')).toBeVisible()
    await expect(modal.getByRole('button', { name: 'Sustituir participante', exact: true })).toHaveCount(0)
    await modal.getByRole('button', { name: 'Cancelar' }).click()
    await expect(modal).not.toBeVisible()

    // Scenario 2b: With candidates, inline validation, and 409 conflict data retention
    mockCandidates = {
      rowVersion: 1,
      actorId: actor.id,
      actorName: actor.displayName,
      canReassign: true,
      candidates: [candidate1, candidate2],
      history: []
    }

    let postCount = 0
    await page.route('**/api/reviews/tasks/' + taskId + '/reassignment', route => {
      if (route.request().method() === 'GET') {
        return route.fulfill({ json: mockCandidates })
      }
      if (route.request().method() === 'POST') {
        postCount++
        return route.fulfill({
          status: 409,
          json: { message: 'La etapa fue modificada concurrentemente por otro usuario.' }
        })
      }
      return route.continue()
    })

    await page.getByRole('button', { name: 'Sustituir participante de Revisión técnica', exact: true }).click()
    await expect(modal).toBeVisible()

    // Candidates combo must contain only returned candidates
    const select = modal.getByLabel(/Nuevo participante/i)
    await expect(select.locator('option')).toHaveCount(3) // 1 placeholder + 2 candidates
    await select.selectOption(candidate1.id)

    // Reason validation & input
    const reasonInput = modal.getByLabel(/Motivo justificado de la sustitución/i)
    await reasonInput.fill('Licencia médica prolongada debidamente justificada.')

    // Submit -> triggers 409
    await modal.getByRole('button', { name: 'Sustituir participante', exact: true }).click()
    expect(postCount).toBe(1)

    // Verify 409 message displayed AND data retained
    await expect(modal.getByText(/La tarea fue modificada concurrentemente/)).toBeVisible()
    await expect(select).toHaveValue(candidate1.id)
    await expect(reasonInput).toHaveValue('Licencia médica prolongada debidamente justificada.')
    await expect(modal.getByRole('button', { name: 'Recargar opciones' })).toBeVisible()

    await withConfirmation(page, true, () => modal.getByRole('button', { name: 'Cancelar' }).click())
    await expect(modal).not.toBeVisible()
  })

  test('3. cancel and escape guard unsaved changes with single confirmation; clean form exits directly', async ({ page }) => {
    const actor = await login(page, 'dev.coordinator')
    const reviewId = randomUUID(), taskId = randomUUID()
    const docTitle = 'Plan de Pruebas Unitarias'
    const candidate = { id: randomUUID(), name: 'Dra. María Elena Quispe' }

    await page.route('**/api/reviews/inbox?*', route => route.fulfill({
      json: {
        items: [{ id: taskId, reviewId, documentId: randomUUID(), title: docTitle, groupName: 'Comisión', periodName: 'Julio–Diciembre 2026', stageLabel: 'Revisión', status: 'ACTIVE', roundNumber: 1 }],
        total: 1, page: 0, size: 20, metrics: { active: 1, completed: 0, returned: 0 }
      }
    }))
    await page.route('**/api/reviews/' + reviewId, route => route.fulfill({
      json: {
        review: { id: reviewId, title: docTitle, status: 'IN_REVIEW', round_number: 1 },
        tasks: [{ id: taskId, actor_id: actor.id, actor_name: actor.displayName, stage_label: 'Revisión', status: 'ACTIVE', row_version: 1, can_reassign: true }],
        observations: [], pageCount: 1, pdfHash: 'e'.repeat(64), canAct: true
      }
    }))
    await page.route('**/api/reviews/' + reviewId + '/pages/*', route => route.fulfill({
      contentType: 'image/png', body: Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a9XcAAAAASUVORK5CYII=', 'base64')
    }))
    await page.route('**/api/reviews/tasks/' + taskId + '/reassignment', route => route.fulfill({
      json: { rowVersion: 1, actorId: actor.id, actorName: actor.displayName, canReassign: true, candidates: [candidate], history: [] }
    }))

    await page.getByRole('button', { name: 'Revisión documental', exact: true }).click()
    await page.getByRole('button', { name: 'Revisar ' + docTitle + ' · Ronda 1', exact: true }).click()

    // 1. Clean form: Cancel button closes immediately without confirmation
    await page.getByRole('button', { name: 'Sustituir participante de Revisión', exact: true }).click()
    const modal = page.locator('.task-reassignment-dialog')
    await expect(modal).toBeVisible()
    await modal.getByRole('button', { name: 'Cancelar' }).click()
    await expect(modal).not.toBeVisible()

    // 2. Clean form: Escape closes immediately
    await page.getByRole('button', { name: 'Sustituir participante de Revisión', exact: true }).click()
    await expect(modal).toBeVisible()
    await page.keyboard.press('Escape')
    await expect(modal).not.toBeVisible()

    // 3. Dirty form: Cancel triggers confirmation; rejecting stays open
    await page.getByRole('button', { name: 'Sustituir participante de Revisión', exact: true }).click()
    await expect(modal).toBeVisible()
    await modal.getByLabel(/Motivo justificado de la sustitución/i).fill('Motivo pendiente')
    await withConfirmation(page, false, () => modal.getByRole('button', { name: 'Cancelar' }).click())
    await expect(modal).toBeVisible()
    await expect(modal.getByLabel(/Motivo justificado de la sustitución/i)).toHaveValue('Motivo pendiente')

    // 4. Dirty form: Escape triggers confirmation; accepting closes directly with single confirm
    await withConfirmation(page, true, () => page.keyboard.press('Escape'))
    await expect(modal).not.toBeVisible()
  })

  test('4. reassignment updates stage and history while preserving observations and preview reference', async ({ page }) => {
    const actor = await login(page, 'dev.coordinator')
    const reviewId = randomUUID(), taskId = randomUUID(), obsId = randomUUID()
    const docTitle = 'Plan de Innovación Curricular'
    const substitute = { id: randomUUID(), name: 'Dra. María Elena Quispe' }
    let requestKeys: string[] = []

    await page.route('**/api/reviews/inbox?*', route => route.fulfill({
      json: {
        items: [{ id: taskId, reviewId, documentId: randomUUID(), title: docTitle, groupName: 'Comisión', periodName: 'Julio–Diciembre 2026', stageLabel: 'Revisión', status: 'ACTIVE', roundNumber: 1 }],
        total: 1, page: 0, size: 20, metrics: { active: 1, completed: 0, returned: 0 }
      }
    }))

    let currentReview = {
      review: { id: reviewId, title: docTitle, status: 'IN_REVIEW', round_number: 1 },
      tasks: [{
        id: taskId,
        actor_id: actor.id,
        actor_name: actor.displayName,
        stage_label: 'Revisión',
        status: 'ACTIVE',
        row_version: 1,
        can_reassign: true,
        slot_actor_id: null,
        reassignments: [] as any[]
      }],
      observations: [{
        id: obsId,
        actor_id: randomUUID(),
        actor_name: 'Evaluador Inicial',
        message: 'Revisar alineación del objetivo',
        page_index: 0,
        x: null, y: null, width: null, height: null,
        can_edit: false,
        can_delete: false,
        created_at: '2026-10-30T10:00:00Z',
        updated_at: null
      }],
      pageCount: 1,
      pdfHash: 'f'.repeat(64),
      canAct: true
    }

    await page.route('**/api/reviews/' + reviewId, route => route.fulfill({ json: currentReview }))
    await page.route('**/api/reviews/' + reviewId + '/pages/*', route => route.fulfill({
      contentType: 'image/png', body: Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a9XcAAAAASUVORK5CYII=', 'base64')
    }))

    await page.route('**/api/reviews/tasks/' + taskId + '/reassignment', route => {
      if (route.request().method() === 'GET') {
        return route.fulfill({
          json: { rowVersion: 1, actorId: actor.id, actorName: actor.displayName, canReassign: true, candidates: [substitute], history: [] }
        })
      }
      if (route.request().method() === 'POST') {
        const body = route.request().postDataJSON()
        requestKeys.push(body.requestKey)
        // Update review state to reflect substitution
        currentReview = {
          ...currentReview,
          tasks: [{
            ...currentReview.tasks[0],
            actor_id: substitute.id,
            actor_name: substitute.name,
            slot_actor_id: actor.id,
            can_reassign: false,
            reassignments: [{
              id: randomUUID(),
              previous_actor_id: actor.id,
              previous_actor_name: actor.displayName,
              actor_id: substitute.id,
              actor_name: substitute.name,
              authorized_by: actor.id,
              authorized_by_name: actor.displayName,
              reason: body.reason,
              created_at: new Date().toISOString()
            }]
          }]
        }
        return route.fulfill({
          json: {
            rowVersion: 2,
            actorId: substitute.id,
            actorName: substitute.name,
            canReassign: false,
            candidates: [],
            history: currentReview.tasks[0].reassignments
          }
        })
      }
      return route.continue()
    })

    await page.getByRole('button', { name: 'Revisión documental', exact: true }).click()
    await page.getByRole('button', { name: 'Revisar ' + docTitle + ' · Ronda 1', exact: true }).click()

    await page.getByRole('button', { name: 'Sustituir participante de Revisión', exact: true }).click()
    const modal = page.locator('.task-reassignment-dialog')
    await modal.getByLabel(/Nuevo participante/i).selectOption(substitute.id)
    await modal.getByLabel(/Motivo justificado de la sustitución/i).fill('Sustitución por encargo institucional de dirección.')

    await modal.getByRole('button', { name: 'Sustituir participante', exact: true }).click()
    await expect(modal).not.toBeVisible()

    // Verify toast
    await expect(page.getByText('Participante sustituido exitosamente.')).toBeVisible()

    // Verify stage displays substitute actor
    await expect(page.getByText(substitute.name, { exact: true }).first()).toBeVisible()

    // Verify reassignment history block is rendered with full traceability
    const historyBlock = page.locator('.stage-reassignments-history')
    await expect(historyBlock).toBeVisible()
    await expect(historyBlock).toContainText(`${actor.displayName} sustituido por ${substitute.name}`)
    await expect(historyBlock).toContainText('Sustitución por encargo institucional de dirección.')

    // Verify previous observation and author are preserved
    await expect(page.getByText('Revisar alineación del objetivo')).toBeVisible()
    await expect(page.getByText('Evaluador Inicial', { exact: true }).first()).toBeVisible()

    // Verify single valid requestKey was sent
    expect(requestKeys.length).toBe(1)
    expect(requestKeys[0]).toMatch(/^[a-f0-9-]{36}$/)
  })

  test('5. substituted reviewer logs in to sign their turn; session identity preserved and no editor steps exposed', async ({ page }) => {
    const substituteActor = await login(page, 'dev.reviewer')
    const reviewId = randomUUID(), taskId = randomUUID()
    const docTitle = 'Plan de Seguimiento de Tutorías'

    await page.route('**/api/reviews/inbox?*', route => route.fulfill({
      json: {
        items: [{ id: taskId, reviewId, documentId: randomUUID(), title: docTitle, groupName: 'Comisión', periodName: 'Julio–Diciembre 2026', stageLabel: 'Revisión y firma', action: 'SIGN', requiresSignature: true, status: 'ACTIVE', roundNumber: 1 }],
        total: 1, page: 0, size: 20, metrics: { active: 1, completed: 0, returned: 0 }
      }
    }))

    await page.route('**/api/reviews/' + reviewId, route => route.fulfill({
      json: {
        review: { id: reviewId, title: docTitle, status: 'IN_REVIEW', round_number: 1 },
        tasks: [{
          id: taskId,
          actor_id: substituteActor.id,
          actor_name: substituteActor.displayName,
          stage_label: 'Revisión y firma',
          requires_signature: true,
          status: 'ACTIVE',
          row_version: 1,
          can_reassign: false,
          slot_actor_id: randomUUID(),
          reassignments: [{
            id: randomUUID(),
            previous_actor_id: randomUUID(),
            previous_actor_name: 'Dr. Titular Original',
            actor_id: substituteActor.id,
            actor_name: substituteActor.displayName,
            authorized_by: randomUUID(),
            authorized_by_name: 'Coordinador Académico',
            reason: 'Delegación oficial de firma',
            created_at: '2026-10-30T11:00:00Z'
          }]
        }],
        observations: [],
        pageCount: 1,
        pdfHash: '1'.repeat(64),
        canAct: true
      }
    }))

    await page.route('**/api/reviews/' + reviewId + '/pages/*', route => route.fulfill({
      contentType: 'image/png', body: Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a9XcAAAAASUVORK5CYII=', 'base64')
    }))

    await page.getByRole('button', { name: 'Revisión documental', exact: true }).click()
    await page.getByRole('button', { name: 'Revisar ' + docTitle + ' · Ronda 1', exact: true }).click()

    // Verify session identity remains Carlos Eduardo Benítez Paredes (substituteActor.displayName)
    await expect(page.locator('.sidebar-account')).toContainText(substituteActor.displayName)

    // Substitute reviewer sees "Preparar firma de revisión" and "Devolver para corrección"
    await expect(page.getByRole('button', { name: 'Preparar firma de revisión', exact: true })).toBeVisible()
    await expect(page.getByRole('button', { name: 'Devolver para corrección', exact: true })).toBeVisible()

    // Substitute reviewer does NOT see document editing tabs or steps (wizard navigation)
    await expect(page.locator('.document-steps')).toHaveCount(0)
    await expect(page.getByRole('button', { name: /Actividades y matriz/i })).toHaveCount(0)
  })

  test('6. career copy only for eligible drafts; dirty content prevents copy action until saved', async ({ page }) => {
    const actor = await login(page, 'dev.principal')
    const planId = randomUUID()
    const originTitle = 'Plan de Gestión de Laboratorios'

    const plan = {
      id: planId,
      title: originTitle,
      groupName: 'Comisión de Laboratorios',
      periodName: 'Julio–Diciembre 2026',
      documentState: 'DRAFT',
      documentType: 'T1' as const,
      editable: true,
      formalVersion: '1.0',
      reviewRound: 1,
      rowVersion: 3,
      teacherId: actor.id,
      teacherName: actor.displayName,
      scopeId: randomUUID(),
      career: 'Ingeniería de Software',
      institutionalUnit: 'Facultad de Ingeniería en Sistemas',
      institutionalUnitType: 'ACADEMIC',
      justification: 'Justificación original del plan.',
      objective: 'Objetivo académico original.',
      preparationDate: '2026-10-30',
      canManage: true,
      archivedAt: null
    }

    await page.route('**/api/work-plans/' + planId, route => {
      if (route.request().method() === 'GET') {
        return route.fulfill({ json: plan })
      }
      if (route.request().method() === 'PUT') {
        const body = route.request().postDataJSON()
        return route.fulfill({ json: { ...plan, ...body, rowVersion: plan.rowVersion + 1 } })
      }
      return route.continue()
    })

    await page.route('**/api/work-plans/' + planId + '/career-copies', route => route.fulfill({
      json: {
        rowVersion: 3,
        canCopy: true,
        scopes: [{
          id: randomUUID(),
          groupId: randomUUID(),
          periodId: randomUUID(),
          groupName: 'Comisión de Laboratorios',
          periodName: 'Julio–Diciembre 2026',
          careerName: 'Tecnologías de la Información',
          canCreate: true,
          suggestedTitle: 'Plan de Gestión de Laboratorios - TI',
          institutionalUnit: 'Facultad de Ingeniería en Sistemas',
          institutionalUnitType: 'ACADEMIC'
        }]
      }
    }))

    await page.getByRole('button', { name: 'Documentos', exact: true }).click()
    // Navigate into the editor using direct URL parameter
    await page.goto('/?doc=' + planId)
    await expect(page.getByRole('heading', { name: originTitle, exact: true })).toBeVisible()

    const copyBtn = page.getByRole('button', { name: 'Copiar a otra carrera', exact: true })
    await expect(copyBtn).toBeVisible()
    await expect(copyBtn).toBeEnabled()

    // Make draft dirty by editing title
    const titleInput = page.getByLabel('Título del Plan', { exact: true })
    await titleInput.fill(originTitle + ' (Modificado)')

    // Dirty content disables the copy action to protect rowVersion and uncommitted matrix
    await expect(copyBtn).toBeDisabled()
    await expect(copyBtn).toHaveAttribute('title', 'Guarde los cambios pendientes antes de copiar a otra carrera')

    // Save changes
    await page.getByRole('button', { name: 'Guardar borrador', exact: true }).click()
    await expect(page.getByText('Borrador guardado en el servidor.')).toBeVisible()
    await expect(copyBtn).toBeEnabled()

    // Clicking copy opens CareerCopyModal
    await copyBtn.click()
    const copyModal = page.locator('.career-copy-dialog')
    await expect(copyModal).toBeVisible()
    await expect(copyModal.getByText('Plan origen:', { exact: false })).toBeVisible()
    await expect(copyModal.getByText('Tecnologías de la Información', { exact: true }).first()).toBeVisible()

    await copyModal.getByRole('button', { name: 'Cancelar' }).click()
    await expect(copyModal).not.toBeVisible()
  })

  test('7. career copy opens returned documents independently', async ({ page }) => {
    const actor = await login(page, 'dev.principal')
    const originPlanId = randomUUID()
    const copyDocId1 = randomUUID()
    const copyDocId2 = randomUUID()
    const scopeId1 = randomUUID()
    const scopeId2 = randomUUID()

    const originPlan = {
      id: originPlanId,
      title: 'Plan Matriz de Vinculación',
      groupName: 'Comisión de Vinculación',
      periodName: 'Julio–Diciembre 2026',
      documentState: 'DRAFT',
      documentType: 'T1' as const,
      editable: true,
      formalVersion: '1.0',
      reviewRound: 1,
      rowVersion: 1,
      teacherId: actor.id,
      teacherName: actor.displayName,
      scopeId: randomUUID(),
      career: 'Ingeniería de Software',
      institutionalUnit: 'Facultad de Ingeniería',
      institutionalUnitType: 'ACADEMIC',
      justification: 'Justificación compartida.',
      objective: 'Objetivo compartido.',
      preparationDate: '2026-10-30',
      canManage: true,
      archivedAt: null
    }

    const copiedPlan1 = {
      ...originPlan,
      id: copyDocId1,
      title: 'Plan Matriz de Vinculación - TI',
      career: 'Tecnologías de la Información',
      scopeId: scopeId1,
      rowVersion: 1
    }

    await page.route('**/api/work-plans/' + originPlanId, route => route.fulfill({ json: originPlan }))
    await page.route('**/api/work-plans/' + copyDocId1, route => route.fulfill({ json: copiedPlan1 }))

    await page.route('**/api/work-plans/' + originPlanId + '/career-copies', route => {
      if (route.request().method() === 'GET') {
        return route.fulfill({
          json: {
            rowVersion: 1,
            canCopy: true,
            scopes: [
              {
                id: scopeId1,
                groupId: randomUUID(),
                periodId: randomUUID(),
                groupName: 'Comisión de Vinculación',
                periodName: 'Julio–Diciembre 2026',
                careerName: 'Tecnologías de la Información',
                canCreate: true,
                suggestedTitle: 'Plan Matriz de Vinculación - TI',
                institutionalUnit: 'Facultad de Ingeniería',
                institutionalUnitType: 'ACADEMIC'
              },
              {
                id: scopeId2,
                groupId: randomUUID(),
                periodId: randomUUID(),
                groupName: 'Comisión de Vinculación',
                periodName: 'Julio–Diciembre 2026',
                careerName: 'Ciberseguridad',
                canCreate: true,
                suggestedTitle: 'Plan Matriz de Vinculación - Ciberseguridad',
                institutionalUnit: 'Facultad de Ingeniería',
                institutionalUnitType: 'ACADEMIC'
              }
              , ...Array.from({ length: 10 }, (_, index) => ({ id: randomUUID(), careerName: 'Carrera adicional ' + index, groupName: 'Comisión de Vinculación', suggestedTitle: 'Plan adicional' }))
            ]
          }
        })
      }
      if (route.request().method() === 'POST') {
        const body = route.request().postDataJSON()
        expect(body.targetScopeIds).toEqual([scopeId1, scopeId2])
        return route.fulfill({
          json: { documentIds: [copyDocId1, copyDocId2] }
        })
      }
      return route.continue()
    })

    await page.goto('/?doc=' + originPlanId)
    await page.getByRole('button', { name: 'Copiar a otra carrera', exact: true }).click()

    const modal = page.locator('.career-copy-dialog')
    await expect(modal).toBeVisible()

    // Select all available destination careers
    await modal.getByRole('button', { name: 'Seleccionar primeras 10' }).click()
    await expect(modal.getByRole('checkbox', { checked: true })).toHaveCount(10)
    await expect(modal.getByRole('checkbox', { checked: false }).first()).toBeDisabled()
    await modal.getByRole('button', { name: 'Deseleccionar todas' }).click()
    await modal.getByLabel(/Tecnologías de la Información/).check()
    await modal.getByLabel(/Ciberseguridad/).check()
    const submitBtn = modal.getByRole('button', { name: 'Copiar a 2 carrera(s)' })
    await expect(submitBtn).toBeVisible()
    await submitBtn.click()

    // Success view shows summary of created documents
    await expect(modal.getByText('Copia multicarrera completada')).toBeVisible()
    await expect(modal.getByText(/Se crearon 2 nuevos borradores independientes/)).toBeVisible()
    await expect(modal.getByRole('button', { name: 'Cancelar: Copiar Plan a otra carrera' })).toBeEnabled()
    await page.screenshot({ path: 'test-results/career-copy-success-desktop.png', fullPage: true })

    // Open first created document
    const openBtns = modal.getByRole('button', { name: 'Abrir documento' })
    await expect(openBtns).toHaveCount(2)
    await openBtns.first().click()

    // Navigated to copiedPlan1 with independent identity
    await expect(modal).not.toBeVisible()
    await expect(page.getByRole('heading', { name: 'Plan Matriz de Vinculación - TI', exact: true })).toBeVisible()
    await expect(page.getByLabel('Carrera, cuando corresponda', { exact: true })).toHaveValue('Tecnologías de la Información')
  })

  test('8. career copy 409 conflict retains target selections and same requestKey on retry', async ({ page }) => {
    const actor = await login(page, 'dev.principal')
    const planId = randomUUID()
    const scopeId = randomUUID()
    let requestKeys: string[] = []

    await page.route('**/api/work-plans/' + planId, route => route.fulfill({
      json: {
        id: planId,
        title: 'Plan de Rediseño Curricular',
        groupName: 'Comisión Curricular',
        periodName: 'Julio–Diciembre 2026',
        documentState: 'DRAFT',
        documentType: 'T1',
        editable: true,
        formalVersion: '1.0',
        reviewRound: 1,
        rowVersion: 1,
        teacherId: actor.id,
      teacherName: actor.displayName,
        scopeId: randomUUID(),
        career: 'Software',
        institutionalUnit: 'FISEI',
        institutionalUnitType: 'ACADEMIC',
        justification: 'J', objective: 'O',
        preparationDate: '2026-10-30',
        canManage: true,
        archivedAt: null
      }
    }))

    await page.route('**/api/work-plans/' + planId + '/career-copies', route => {
      if (route.request().method() === 'GET') {
        return route.fulfill({
          json: {
            rowVersion: 1,
            canCopy: true,
            scopes: [{
              id: scopeId,
              groupId: randomUUID(),
              periodId: randomUUID(),
              groupName: 'Comisión Curricular',
              periodName: 'Julio–Diciembre 2026',
              careerName: 'Robótica',
              canCreate: true,
              suggestedTitle: 'Plan de Rediseño Curricular - Robótica',
              institutionalUnit: 'FISEI',
              institutionalUnitType: 'ACADEMIC'
            }]
          }
        })
      }
      if (route.request().method() === 'POST') {
        const body = route.request().postDataJSON()
        requestKeys.push(body.requestKey)
        return route.fulfill({
          status: 409,
          json: { message: 'El ámbito de destino ya tiene un Plan vigente registrado concurrentemente.' }
        })
      }
      return route.continue()
    })

    await page.goto('/?doc=' + planId)
    await page.getByRole('button', { name: 'Copiar a otra carrera', exact: true }).click()

    const modal = page.locator('.career-copy-dialog')
    await modal.getByLabel(/Robótica/).check()

    // Submit triggers 409
    await modal.getByRole('button', { name: 'Copiar a 1 carrera(s)' }).click()

    // Verify 409 error displayed, checkbox remains checked, and reload button offered
    await expect(modal.getByText(/El documento o alguno de los destinos cambió concurrentemente/)).toBeVisible()
    await expect(modal.getByLabel(/Robótica/)).toBeChecked()
    await expect(modal.getByRole('button', { name: 'Recargar opciones' })).toBeVisible()

    // Retry with same selection retains identical requestKey
    await modal.getByRole('button', { name: 'Copiar a 1 carrera(s)' }).click()
    expect(requestKeys.length).toBe(2)
    expect(requestKeys[0]).toBe(requestKeys[1])

    await withConfirmation(page, true, () => modal.getByRole('button', { name: 'Cancelar' }).click())
    await expect(modal).not.toBeVisible()
  })

  test('9. filter preservation, documents navigation and on-demand query execution without loops', async ({ page }) => {
    await login(page, 'dev.principal')
    let careerCopiesCalls = 0
    let reassignmentCalls = 0

    await page.route('**/api/work-plans/**/career-copies', route => {
      careerCopiesCalls++
      return route.fulfill({ json: { rowVersion: 1, canCopy: false, scopes: [] } })
    })

    await page.route('**/api/reviews/tasks/**/reassignment', route => {
      reassignmentCalls++
      return route.fulfill({ json: { rowVersion: 1, actorId: actor.id, actorName: actor.displayName, canReassign: false, candidates: [], history: [] } })
    })

    // Navigate to Documentos
    await page.getByRole('button', { name: 'Documentos', exact: true }).click()
    await expect(page.getByRole('heading', { name: 'Mis documentos', exact: true })).toBeVisible()

    // Neither career-copies nor reassignment should be called merely by viewing documents table
    expect(careerCopiesCalls).toBe(0)
    expect(reassignmentCalls).toBe(0)

    // Apply a search filter
    const searchInput = page.getByRole('searchbox', { name: 'Buscar por título o ámbito' })
    if (await searchInput.isVisible()) {
      await searchInput.fill('Filtro de prueba')
      await page.keyboard.press('Enter')
      await expect(searchInput).toHaveValue('Filtro de prueba')
    }
  })

  test('10. keyboard accessibility, focus containment, dark mode and tablet layout for reassignment', async ({ page }) => {
    const actor = await login(page, 'dev.coordinator')
    const reviewId = randomUUID(), taskId = randomUUID()
    const docTitle = 'Plan de Titulación y Graduación con Nombre Extenso para Validar Truncamiento y Ajuste Responsivo'
    const candidate = { id: randomUUID(), name: 'Dra. María Elena Quispe' }

    await page.route('**/api/reviews/inbox?*', route => route.fulfill({
      json: {
        items: [{ id: taskId, reviewId, documentId: randomUUID(), title: docTitle, groupName: 'Comisión de Titulación', periodName: 'Julio–Diciembre 2026', stageLabel: 'Revisión técnica', status: 'ACTIVE', roundNumber: 1 }],
        total: 1, page: 0, size: 20, metrics: { active: 1, completed: 0, returned: 0 }
      }
    }))
    await page.route('**/api/reviews/' + reviewId, route => route.fulfill({
      json: {
        review: { id: reviewId, title: docTitle, status: 'IN_REVIEW', round_number: 1 },
        tasks: [{ id: taskId, actor_id: actor.id, actor_name: actor.displayName, stage_label: 'Revisión técnica', status: 'ACTIVE', row_version: 1, can_reassign: true }],
        observations: [], pageCount: 1, pdfHash: 'a'.repeat(64), canAct: true
      }
    }))
    await page.route('**/api/reviews/' + reviewId + '/pages/*', route => route.fulfill({
      contentType: 'image/png', body: Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a9XcAAAAASUVORK5CYII=', 'base64')
    }))
    await page.route('**/api/reviews/tasks/' + taskId + '/reassignment', route => route.fulfill({
      json: { rowVersion: 1, actorId: actor.id, actorName: actor.displayName, canReassign: true, candidates: [candidate], history: [] }
    }))

    await page.getByRole('button', { name: 'Revisión documental', exact: true }).click()
    await page.getByRole('button', { name: 'Revisar ' + docTitle + ' · Ronda 1', exact: true }).click()

    const triggerBtn = page.getByRole('button', { name: 'Sustituir participante de Revisión técnica', exact: true })
    await triggerBtn.click()

    const modal = page.locator('.task-reassignment-dialog')
    await expect(modal).toBeVisible()

    // 1. Focus trap inside modal: pressing Tab stays within dialog
    for (let i = 0; i < 6; i++) {
      await page.keyboard.press('Tab')
      const isInModal = await page.evaluate(() => document.activeElement?.closest('.task-reassignment-dialog') !== null)
      expect(isInModal).toBe(true)
    }

    // 2. Dark mode testing
    const themeToggle = page.getByRole('button', { name: 'Modo oscuro', exact: true })
    if (await themeToggle.isVisible()) {
      await themeToggle.click()
      await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark')
      // Modal remains opaque and visible with contrast
      await expect(modal).toBeVisible()
      const lightToggle = page.getByRole('button', { name: 'Modo claro', exact: true })
      if (await lightToggle.isVisible()) await lightToggle.click()
    }

    // 3. Tablet layout (820px)
    await page.setViewportSize({ width: 820, height: 1180 })
    await expect(modal).toBeVisible()
    const modalBox = await modal.boundingBox()
    expect(modalBox?.width).toBeLessThanOrEqual(820)

    await page.screenshot({ path: 'test-results/reassignment-tablet.png', fullPage: true })

    // 4. Close modal and verify focus restores to trigger button
    await modal.getByRole('button', { name: 'Cancelar' }).click()
    await expect(modal).not.toBeVisible()
    await expect(triggerBtn).toBeFocused()
  })
})


test('report and organization filters follow reload and browser navigation, including cleared selections', async ({ page }) => {
  await login(page, 'dev.principal')
  const periods = await api(page, '/periods')
  const scopes = await api(page, '/organization/scopes')
  expect(periods.length).toBeGreaterThan(0)
  expect(scopes.length).toBeGreaterThan(0)
  await page.getByRole('button', { name: 'Reportes de gestión', exact: true }).click()
  const period = page.getByLabel('Período del reporte', { exact: true })
  await period.selectOption(periods[0].id)
  await expect(page).toHaveURL(new RegExp('period=' + periods[0].id))
  await page.reload()
  await expect(period).toHaveValue(periods[0].id)
  await page.evaluate(() => {
    history.pushState(null, '', '?view=reporting')
    dispatchEvent(new PopStateEvent('popstate'))
  })
  await expect(period).toHaveValue('')
  await expect.poll(() => new URL(page.url()).searchParams.has('period')).toBe(false)
  await page.getByRole('button', { name: 'Mi organización', exact: true }).click()
  const scope = page.getByLabel('Mi ámbito institucional', { exact: true })
  await scope.selectOption(scopes[0].id)
  await page.reload()
  await expect(scope).toHaveValue(scopes[0].id)
  await scope.selectOption(scopes[0].id) // Selecting the same scope must not leave loading stuck.
  await expect(page.getByText('Comprobando autorización…', { exact: true })).toHaveCount(0)
  await page.evaluate(() => {
    history.pushState(null, '', '?view=my-organization')
    dispatchEvent(new PopStateEvent('popstate'))
  })
  await expect(scope).toHaveValue('')
  await expect(page.locator('.organization-scope-summary')).toHaveCount(0)
})
