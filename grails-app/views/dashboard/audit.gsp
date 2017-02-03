<%--
  User: pmcneil
  Date: 15/09/14
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
  <meta name="layout" content="main">
  <title>NSL Dashboard</title>
</head>

<body>
<div class="container">

  <g:form name="search" role="form" controller="dashboard" action="audit" method="GET">
    <div class="form-group">
      <h2>Audit</h2>
      <label>User name
        <input type="text" name="userName" placeholder="Enter a user name" value="${query.userName}"
               class="form-control" size="30"/>
      </label>

      <label>From
        <input type="text" name="fromStr" class="form-control fromDate" value="${query.fromStr}">
      </label>
      <label>To
        <input type="text" name="toStr" class="form-control toDate" value="${query.toStr}">
      </label>
      <button type="submit" name="search" value="true" class="btn btn-primary">Search</button>
    </div>
  </g:form>

  <g:if test="${auditRows == null}">
    <div>
      Your results will be here.
    </div>
  </g:if>
  <g:elseif test="${auditRows.size() > 0}">
  <table class="table">
    <g:each in="${auditRows}" var="row">
      <tr>
        <td>${row.when()}</td>
        <td><b>${row.updatedBy()}</b></td>
        <td>${[U: 'Updated', I: 'Created', D: 'Deleted'].get(row.action)}</td>
        <td>
          <g:if test="${row.auditedObj}">
            <st:nicerDomainString domainObj="${row.auditedObj}"/>
          </g:if>
          <g:else>
            ${"$row.table $row.rowData.id (deleted?)"}
          </g:else>
        </td>
        <td>
          <g:each in="${row.fieldDiffs()}" var="diff">
            <div>
              <div><b>${diff.fieldName}</b></div>

              <div class="diffBefore">${diff.before}</div>

              <div class="diffAfter">${diff.after}</div>
            </div>
          </g:each>
        </td>
      </tr>
    </g:each>
  </table>
  </g:elseif>
  <g:else>
    <div>
      No Results found.
    </div>
  </g:else>

</div>
</body>
</html>