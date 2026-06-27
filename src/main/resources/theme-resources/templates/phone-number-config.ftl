<#import "template.ftl" as layout>
<#import "field.ftl" as field>
<#import "buttons.ftl" as buttons>
<@layout.registrationLayout displayMessage=true; section>
    <#if section = "header">
        ${msg("bulkgatePhoneFormTitle")}
    <#elseif section = "form">
        <div id="kc-form">
            <div id="kc-form-wrapper">
                <p id="kc-bulkgate-phone-instruction" class="${properties.kcInputHelperTextClass!} pf-v5-u-mb-md">
                    ${msg("bulkgatePhoneInstruction")}
                </p>

                <form id="kc-bulkgate-phone-form" class="${properties.kcFormClass!}"
                      onsubmit="submit.disabled = true; return true;" action="${url.loginAction}" method="post">

                    <@field.input name="mobileNumber" label=msg("bulkgatePhoneLabel")
                        value=(mobileNumber!'') autocomplete="tel" autofocus=true error="" />

                    <@buttons.actionGroup>
                        <@buttons.button id="kc-submit" name="submit" label="bulkgateSubmit"
                            class=["kcButtonPrimaryClass", "kcButtonBlockClass"] />
                    </@buttons.actionGroup>
                </form>
            </div>
        </div>
    </#if>
</@layout.registrationLayout>
