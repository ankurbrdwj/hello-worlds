package com.ankur.brains.jcp.usage;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/admin/reconciliations")
class ReconciliationController {

	private final ReconciliationService reconciliationService;

	ReconciliationController(ReconciliationService reconciliationService) {
		this.reconciliationService = reconciliationService;
	}

	@PostMapping
	ReconciliationReport reconcile(@RequestParam(defaultValue = "true") boolean repair) {
		return reconciliationService.reconcile(repair);
	}

}
