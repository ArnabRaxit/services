<%@ page import="au.org.biodiversity.nsl.Instance; au.org.biodiversity.nsl.ConfigService" %>
<!DOCTYPE html>
<html>
<head>
  <meta name="layout" content="main">
  <title>${instance.name.simpleName}, ${instance.reference.citation}</title>
</head>

<body>
<h1>Usage of a name <span class="small">(Instance)</span> <help>
  <i class="fa fa-info-circle"></i>

  <div>
    An instance of a name in a reference, or usage of a name.
    <ul>
      <li>At the bottom of this page are the citable links to this Instance object or just use the <i
          class="fa fa-link"></i> icon.
      You can "right click" in most browsers to copy it or open it in a new browser tab.</li>
    </ul>
  </div>
</help>
</h1>

<div class="rest-resource-content">

  <div data-nameId="${instance.name.id}">
    %{--do not reformat the next line it inserts a space between the comma and the fullName--}%
    <b><st:preferedLink target="${instance.name}"><st:encodeWithHTML text='${instance.name.fullNameHtml}'/></st:preferedLink></b><name-status
      class="${instance.name.nameStatus.name}">, ${instance.name.nameStatus.name}</name-status><name-type
      class="${instance.name.nameType.name}">, ${instance.name.nameType.name}</name-type>
    <span class="text-muted small">${instance?.instanceType?.name}</span>
    <editor>
      <st:editorLink nameId="${instance.name.id}"><i class="fa fa-edit" title="Edit"></i></st:editorLink>
    </editor>

    <af:apniLink name="${instance.name}"/>
    <span class="vertbar">
      <st:preferedLink target="${instance}"><i class="fa fa-link"></i></st:preferedLink>
    </span>
  </div>

  <reference data-referenceId="${instance.reference.id}">
    <ref-citation>
      %{--don't reformat the citationHtml line--}%
      <st:preferedLink target="${instance.reference}"><st:encodeWithHTML text='${instance.reference?.citationHtml}'/></st:preferedLink>:
    </ref-citation>

    <page><af:page instance="${instance}"/></page>

    <g:if test="${(apc as au.org.biodiversity.nsl.Node)?.taxonUriIdPart == instance.id.toString()}">
      <g:if test="${(apc as au.org.biodiversity.nsl.Node)?.typeUriIdPart == 'ApcConcept'}">
        <apc><i class="fa fa-check"></i>${ConfigService.classificationTreeName}</apc>
      </g:if>
      <g:else>
        <apc><i class="fa fa-ban"></i>${ConfigService.classificationTreeName}</apc>
      </g:else>
    </g:if>
    <instance-type class="${instance?.instanceType?.name}">[${instance?.instanceType?.name}]</instance-type>

    <st:preferedLink target="${instance.reference}"><i class="fa fa-book"></i></st:preferedLink>

    <span class="vertbar">
      <a href="${af.refNameTreeSearchLink(citation: instance.reference?.citation, product: params.product)}"
         title="Search for names in this reference.">
        <i class="fa fa-search"></i></a>
    </span>

    <instance data-instanceId="${instance.id}">

      <ul class="instance-notes list-unstyled">
        <af:getTypeNotes instance="${instance}" var="instanceNote">
          <li>
            <instance-note-key
                class="${instanceNote.instanceNoteKey.name}">${instanceNote.instanceNoteKey.name}:</instance-note-key>
            <instance-note>${instanceNote.value}</instance-note>
          </li>
        </af:getTypeNotes>
      </ul>

      <g:if test="${instance.instanceType.synonym || instance.instanceType.unsourced}">
        <g:render template="/apniFormat/synonymOf" model="[instance: instance]"/>
      </g:if>

      <g:if test="${instance.cites}">
        <has-synonym>
          Cites <synonym-type
            class="${instance.cites.instanceType.name}">${instance.cites.instanceType.name}:</synonym-type>
          <st:preferedLink target="${instance.cites}"><st:encodeWithHTML text='${instance.cites.name.fullNameHtml}'/></st:preferedLink>
          <name-status
              class="${instance.cites.name.nameStatus.name}">${instance.cites.name.nameStatus.name}</name-status>

          <af:apniLink name="${instance.cites.name}"/>

        </has-synonym>
      </g:if>

      <g:if test="${instance.instancesForCitedBy}">
        <g:render template="/apniFormat/hasSynonym"
                  model="[instances: instance.instancesForCitedBy.findAll { it.instanceType.synonym }]"/>
        <g:render template="/apniFormat/missapplication"
                  model="[instances: instance.instancesForCitedBy.findAll {
                    it.instanceType.name.contains('misapplied')
                  }]"/>
      %{--other synonyms--}%
        <g:render template="/apniFormat/hasSynonym" model="[instances: instance.instancesForCitedBy.findAll {
          (!it.instanceType.synonym && !it.instanceType.name.contains('misapplied'))
        }]"/>
      </g:if>

      <g:if test="${instance.instanceType.misapplied}">
        <g:render template="/apniFormat/missappliedTo" model="[instance: instance]"/>
      </g:if>


      <ul class="instance-notes list-unstyled">
        <af:getDisplayableNonTypeNotes instance="${instance}" var="instanceNote">
          <li>
            <instance-note-key
                class="${instanceNote.instanceNoteKey.name}">${instanceNote.instanceNoteKey.name}:</instance-note-key>
            <instance-note>${instanceNote.value}</instance-note>
          </li>
        </af:getDisplayableNonTypeNotes>
      </ul>

      <g:if test="${instance.instanceType.secondaryInstance}">
        <h4>Secondary reference of</h4>
        <st:primaryInstance var="primaryInstance" name="${instance.name}">
          <st:preferedLink target="${primaryInstance}"><st:encodeWithHTML text='${primaryInstance.name.fullNameHtml}'/></st:preferedLink>
        </st:primaryInstance>
      </g:if>

      <g:if test="${instance.instancesForCites}">
        <h4>Nomenclatural links</h4>
        <af:sortedInstances instances="${instance.instancesForCites.findAll { it.instanceType.synonym }}" var="synonym">
          <g:render template="/apniFormat/synonymOf" model="[instance: synonym]"/>
        </af:sortedInstances>

        <g:render template="/apniFormat/missapplication"
                  model="[instances: instance.instancesForCites.findAll { it.instanceType.misapplied }]"/>
      </g:if>

    </instance>
  </reference>

  <div id="foaToggle" class="toggleNext" style="display:none;">
    Flora of Australia
    <i class="fa fa-caret-right"></i>
    <i class="fa fa-caret-down" style="display: none;"></i>
  </div>

  <div style="display: none;">
    <foa data-id="${st.mapperUrl()}/boa/foa/taxa/${instance.id}"></foa>
  </div>
  <g:render template="links"/>
</div>
</body>
</html>

