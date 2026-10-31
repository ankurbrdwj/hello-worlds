package com.ankur.brains.jcp.usage;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/usage-events")
class UsageEventController {

	private final UsageService usageService;

	UsageEventController(UsageService usageService) {
		this.usageService = usageService;
	}

	@PostMapping
	ResponseEntity<UsageReceipt> record(@RequestBody UsageEvent event) {
		UsageOutcome outcome = usageService.record(event);
		UsageReceipt receipt = new UsageReceipt(event.getEventKey(), outcome.getUsedAfter());
		switch (outcome.getResult()) {
		case RECORDED:
			return ResponseEntity.status(HttpStatus.CREATED).body(receipt);
		case DUPLICATE:
			return ResponseEntity.ok(receipt);
		case KEY_REUSED_WITH_DIFFERENT_CONTENT:
			return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
		default:
			return ResponseEntity.status(HttpStatus.CONFLICT).build();
		}
	}

}
