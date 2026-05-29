package com.lalkalol.i18n

import freemarker.template.SimpleScalar
import freemarker.template.TemplateMethodModelEx

class MsgMethod(private val locale: UiLocale) : TemplateMethodModelEx {
    override fun exec(arguments: List<*>?): Any {
        val args = arguments.orEmpty().map { it.toString() }
        val key = args.first()
        val params = args.drop(1).toTypedArray()
        return SimpleScalar(Messages.t(locale, key, *params))
    }
}
