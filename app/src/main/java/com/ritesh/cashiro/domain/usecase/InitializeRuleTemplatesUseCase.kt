package com.ritesh.cashiro.domain.usecase

import com.ritesh.cashiro.domain.repository.RuleRepository
import com.ritesh.cashiro.domain.service.RuleTemplateService
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class InitializeRuleTemplatesUseCase @Inject constructor(
    private val ruleRepository: RuleRepository,
    private val ruleTemplateService: RuleTemplateService
) {
    suspend operator fun invoke(forceReset: Boolean = false) {
        val existingRules = ruleRepository.getAllRules().first()

        if (existingRules.isEmpty() || forceReset) {
            // Delete all existing rules when force resetting to avoid duplicates
            if (forceReset) {
                ruleRepository.deleteAllRules()
            }

            val templates = ruleTemplateService.getDefaultRuleTemplates()
            templates.forEach { template ->
                ruleRepository.insertRule(template)
            }
        }
    }
}