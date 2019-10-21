<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>

<jsp:useBean id="keys" class="com.rigor.teamcity.optimization.OptimizationConstants" />

<p>
    Below are the currently defined test parameters:
</p>

<div class="parameter">
    Optimization Test IDs: <props:displayValue name="${keys.testIds}" emptyValue="Not set, missing value"/>
</div>
<div class="parameter">
    Minimum Performance Score: <props:displayValue name="${keys.minimumPerformanceScore}" emptyValue="0"/>
</div>
<div class="parameter">
    Maximum Critical Defects Allowed: <props:displayValue name="${keys.maximumCriticalDefectsAllowed}" emptyValue="0"/>
</div>
<div class="parameter">
    Disallowed Defect IDs: <props:displayValue name="${keys.disallowedDefectIds}" emptyValue="-"/>
</div>
<div class="parameter">
    Verify Performance Budgets: <props:displayValue name="${keys.verifyPerformanceBudgets}" emptyValue="false"/>
</div>