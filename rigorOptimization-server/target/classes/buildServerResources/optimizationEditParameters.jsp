<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>

<jsp:useBean id="keys" class="com.rigor.teamcity.optimization.OptimizationConstants" />
<jsp:useBean id="propertiesBean" scope="request" type="jetbrains.buildServer.controllers.BasePropertiesBean"/>

<l:settingsGroup title="Rigor Optimization Settings">
    <tr>
        <th><label for="${keys.apiKey}">Optimization API Key: <l:star/></label></th>
        <td>
            <div class="posRel">
                <props:textProperty name="${keys.apiKey}" size="56" />
                <span class="error" id="error_${keys.apiKey}"></span>
                <span class="smallNote">To get your API key, <a href="https://optimization.rigor.com/settings/api" target="_blank">click here</a>.</span>
            </div>
        </td>
    </tr>
    <tr>
        <th><label for="${keys.testIds}">Optimization Test IDs: <l:star/></label></th>
        <td>
            <div class="posRel">
                <props:textProperty name="${keys.testIds}" size="56" maxlength="100"/>
                <span class="error" id="error_${keys.testIds}"></span>
                <span class="smallNote">For multiple tests, use a comma separated list.</span>
            </div>
        </td>
    </tr>
    <tr>
        <th><label for="${keys.minimumPerformanceScore}">Minimum Performance Score:</label></th>
        <td>
            <div class="posRel">
                <props:textProperty name="${keys.minimumPerformanceScore}" size="14" maxlength="3"/>
                <span class="error" id="error_${keys.minimumPerformanceScore}"></span>
            </div>
        </td>
    </tr>
    <tr>
        <th><label for="${keys.maximumCriticalDefectsAllowed}">Maximum Critical Defects Allowed:</label></th>
        <td>
            <div class="posRel">
                <props:textProperty name="${keys.maximumCriticalDefectsAllowed}" size="14" maxlength="3"/>
                <span class="error" id="error_${keys.maximumCriticalDefectsAllowed}"></span>
                <span class="smallNote">You can mark a build as failed if the critical defect count is exceeded.</span>
            </div>
        </td>
    </tr>
    <tr>
        <th><label for="${keys.disallowedDefectIds}">Disallowed Defect IDs:</label></th>
        <td>
            <div class="posRel">
                <props:textProperty name="${keys.disallowedDefectIds}" size="56" maxlength="50"/>
                <span class="error" id="error_${keys.disallowedDefectIds}"></span>
                <span class="smallNote">You can block builds from progressing by setting a comma separated list of Rigor defect IDs.</span>
            </div>
        </td>
    </tr>
    <tr>
        <th><label for="${keys.verifyPerformanceBudgets}">Verify Performance Budgets:</label></th>
        <td>
            <div class="posRel">
                <props:checkboxProperty name="${keys.verifyPerformanceBudgets}"/>
                <span class="smallNote">Checking this option will enforce Rigor's performance budgets.</span>
            </div>
        </td>
    </tr>
</l:settingsGroup>