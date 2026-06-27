<#import "template.ftl" as layout>
<#import "field.ftl" as field>
<#import "buttons.ftl" as buttons>
<@layout.registrationLayout displayMessage=true; section>
    <#if section = "header">
        ${msg("bulkgateFormTitle")}
    <#elseif section = "form">
        <div id="kc-form">
            <div id="kc-form-wrapper">
                <p id="kc-sms-otp-instruction" class="${properties.kcInputHelperTextClass!} pf-v5-u-mb-md">
                    ${msg("bulkgateInstruction", (ttlMinutes!5)?c)}
                </p>

                <form id="kc-sms-otp-login-form" class="${properties.kcFormClass!}"
                      onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post">

                    <@field.input name="code" label=msg("bulkgateCodeLabel")
                        autocomplete="one-time-code" autofocus=true error="" />

                    <@buttons.actionGroup>
                        <@buttons.button id="kc-login" name="login" label="bulkgateSubmit"
                            class=["kcButtonPrimaryClass", "kcButtonBlockClass"] />
                        <@buttons.button id="kc-sms-otp-resend" name="resend" label="bulkgateResend"
                            class=["kcButtonSecondaryClass", "kcButtonBlockClass"] value="true" />
                    </@buttons.actionGroup>
                </form>
            </div>
        </div>
    </#if>
</@layout.registrationLayout>
