<#macro script>
<script>
    window.i18n = JSON.parse('${i18nJson?js_string}');
    <#if viewJson??>
    window.initialView = JSON.parse('${viewJson?js_string}');
    </#if>
</script>
</#macro>
