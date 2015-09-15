/*
    Copyright 2015 Australian National Botanic Gardens

    This file is part of NSL services project.

    Licensed under the Apache License, Version 2.0 (the "License"); you may not
    use this file except in compliance with the License. You may obtain a copy
    of the License at http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/

/**
 * This service provides for a flat view of the NSL data for export of various outputs. This replaces the NslSimpleName
 * table.
 *
 * See JIRA NSL-1369
 *
 */
package au.org.biodiversity.nsl

import grails.transaction.Transactional


@Transactional
class FlatViewService {

    def serviceMethod() {
         /*
canonicalName               // Name
scientificNameAuthorship    // authority
ccLicense                   // http://creativecommons.org/licenses/by/3.0/
ccAttributionIRI            // name list e.g https://biodiversity.org.au/nsl/services/name/apni/112770
ccAttributionIRI            // taxon list e.g. https://biodiversity.org.au/nsl/services/instance/apni/594478
scientificName              // taxonName
scientificNameID            // apniURI
cultivarName                // cultivarName
canonicalNameHTML           // simpleNameHtml
scientificNameHTML          // fullNameHtml
nameType                    // nameTypeName
nomenclatural code          // e.g ICNAFP
originalNameUsageID         // protoNameURI
namePublishedIn             // proto reference citation
namePublishedInYear         // protoYear
orginalTaxonomicStatus      // proto instance type
homonym                     // homonym
autonym                     // autonym
hybrid                      // hybrid
cultivar                    // cultivar
formula                     // formula
scientific                  // scientific
nomStat                     // nomStat
nomIlleg                    // nomIlleg
nomInval                    // nomInval
basionymAuthor              // baseNameAuthor
exBasionymAuthor            // exBaseNameAuthor
combinationAuthor           // author
exCombinationAuthor         // exAuthor
sanctioningWork             // sanctioningAuthor
taxonRank                   // rank
taxonRankSortOrder          // rankSortOrder
taxonRankAbbreviation       // rankAbbrev
regnum                      //
classis                     // classis
subclassis                  // subclassis
familia                     // familia
genus                       // genus
species                     // species
infraspecies                // infraspecies
firstHybridParentName       //
firstHybridParentNameID     //
secondHybridParentName      // secondParentNameUsage
secondHybridParentNameID    // secondParentNsl
created                     // createdAt
modified                    // updatedAt
nameElement                 // nameElement

classification              // Classifications
taxonID                     // apcRelationshipInstanceId
acceptedNameUsage           // apcAcceptedName
acceptedNameUsageID         // apcAcceptedNameURI
taxonomicStatus             // apcRelationshipType
nameAccordingToID           // apcInstance.ReferenceID
nameAccordingTo             // apcInstance.Reference
taxonRemarks                // apcComment
taxonDistribution           // apcDistribution
apni                        // apni
higherClassification        // nameTreePath
parentNameUsage             //
parentNameUsageID           // parentNsl
familyTaxonID               // familyNsl
genericTaxonID              // genusNsl
speciesTaxonID              // speciesNsl

excludedName                // apcExcluded
proParteRelationship        // apcProparte

          */
        String query = """
SELECT
  n.id,
  pro.id,
  n.simple_name                                                                                  AS canonical_name,
  regexp_replace(substring(n.full_name_html FROM '<authors>(.*)</authors>'), '<[^>]*>', '', 'g') AS authority,
  n.full_name                                                                                    AS scientific_name,
  'https://biodiversity.org.au/boa/name/apni/' || n.id                                           AS scientific_name_ID,
  CASE WHEN nt.cultivar = TRUE
    THEN n.name_element
  ELSE NULL END                                                                                  AS cultivar_name,
  n.simple_name_html                                                                             AS canonical_name_HTML,
  n.full_name_html                                                                               AS scientific_name_HTML,
  nt.name                                                                                        AS name_type,
  'blah'                                                                                         AS nomenclatural_code,
  pro_ref.citation                                                                               AS name_published_in,
  pro_ref.year                                                                                   AS name_published_year,
  pit.name                                                                                       AS original_taxonomic_status,
  'todo'                                                                                         AS homonym,
  nt.autonym                                                                                     AS autonym,
  nt.hybrid                                                                                      AS hybrid,
  nt.cultivar                                                                                    AS cultivar,
  nt.formula                                                                                     AS formula,
  nt.scientific                                                                                  AS scientific,
  ns.nom_inval                                                                                   AS nom_inval,
  ns.nom_illeg                                                                                   AS nom_illeg,
  ns.name                                                                                        AS nomenclatural_status,
  basionym_author.abbrev                                                                         AS basionym_author,
  ex_basionym_author.abbrev                                                                      AS ex_basionym_author,
  combination_author.abbrev                                                                      AS comibination_author,
  ex_combination_author.abbrev                                                                   AS ex_combination_author,
  sanctioning_work.abbrev                                                                        AS sanctioning_work,
  rank.name                                                                                      AS taxon_rank,
  rank.sort_order                                                                                AS taxon_rank_sort_order,
  rank.abbrev                                                                                    AS taxon_rank_abbreviation,

  substring(ntp.rank_path FROM 'Regnum:([^>]*)')                                                 AS regnum,
  substring(ntp.rank_path FROM 'Classis:([^>]*)')                                                AS classis,
  substring(ntp.rank_path FROM 'Subclassis:([^>]*)')                                             AS subclassis,
  substring(ntp.rank_path FROM 'Familia:([^>]*)')                                                AS familia,
  substring(ntp.rank_path FROM 'Genus:([^>]*)')                                                  AS genus,
  substring(ntp.rank_path FROM 'Species:([^>]*)')                                                AS species,
  substring(ntp.rank_path FROM 'Species:[^>]*>.*:(.*)\\\$')                                        AS infraspecies,

  CASE WHEN firstHybridParent.id IS NOT NULL
    THEN
      firstHybridParent.full_name
  ELSE NULL END                                                                                  AS first_hybrid_parent_name,

  CASE WHEN firstHybridParent.id IS NOT NULL
    THEN
      'https://biodiversity.org.au/boa/name/apni/' || firstHybridParent.id
  ELSE NULL END                                                                                  AS first_hybrid_parent_id,

  CASE WHEN secondHybridParent.id IS NOT NULL
    THEN
      secondHybridParent.full_name
  ELSE NULL END                                                                                  AS second_hybrid_parent_name,

  CASE WHEN secondHybridParent.id IS NOT NULL
    THEN
      'https://biodiversity.org.au/boa/name/apni/' || secondHybridParent.id
  ELSE NULL END                                                                                  AS second_hybrid_parent_id,

  n.created_at                                                                                   AS created,
  n.updated_at                                                                                   AS modified,
  n.name_element                                                                                 AS name_element,
  array(SELECT t2.label
        FROM name_tree_path ntp2 JOIN tree_arrangement t2 ON ntp2.tree_id = t2.id
        WHERE name_id = n.id
        ORDER BY t2.label)                                                                       AS classifications,


  CASE WHEN apc_inst.id IS NOT NULL
    THEN
      'https://biodiversity.org.au/boa/instance/apni/' || apc_inst.id
  ELSE NULL END                                                                                  AS taxon_id,
  accepted_name.full_name                                                                        AS accepted_name_usage,
  CASE WHEN apcn.instance_id IS NOT NULL
    THEN
      'https://biodiversity.org.au/boa/instance/apni/' || apcn.instance_id
  ELSE NULL END                                                                                  AS accepted_name_usage_id,

  apcr.citation                                                                                  AS name_according_to,
  CASE WHEN apcr.citation IS NOT NULL
    THEN
      'https://biodiversity.org.au/boa/reference/apni/' || apcr.citation
  ELSE NULL END                                                                                  AS name_according_to_id,

  apc_comment.value                                                                              AS taxon_remarks,
  apc_dist.value                                                                                 AS taxon_distribution,
  regexp_replace(ntp.name_path, '\\>',
                 ' | ', 'g')                                                                          AS higher_classification


FROM name n
  JOIN name_type nt ON n.name_type_id = nt.id
  JOIN name_status ns ON n.name_status_id = ns.id
  JOIN author combination_author ON combination_author.id = n.author_id
  JOIN name_rank rank ON n.name_rank_id = rank.id

  LEFT OUTER JOIN author basionym_author ON n.base_author_id = basionym_author.id
  LEFT OUTER JOIN author ex_basionym_author ON n.ex_base_author_id = ex_basionym_author.id
  LEFT OUTER JOIN author ex_combination_author ON n.ex_author_id = ex_combination_author.id
  LEFT OUTER JOIN author sanctioning_work ON n.sanctioning_author_id = sanctioning_work.id

  LEFT OUTER JOIN instance pro
  JOIN instance_type pit ON pit.id = pro.instance_type_id
  JOIN reference pro_ref ON pro.reference_id = pro_ref.id
    ON pit.id = pro.instance_type_id AND pro.name_id = n.id AND pit.primary_instance = TRUE

  LEFT OUTER JOIN instance apc_inst
  JOIN tree_node apcn ON apcn.instance_id = apc_inst.id OR apcn.instance_id = apc_inst.cited_by_id
  JOIN tree_arrangement tree
    ON
      tree.id = apcn.tree_arrangement_id
      AND tree.label = 'APC'
      AND apcn.checked_in_at_id IS NOT NULL
      AND apcn.next_node_id IS NULL
    ON apc_inst.name_id = n.id

  LEFT OUTER JOIN reference apcr ON apc_inst.reference_id = apcr.id
  LEFT OUTER JOIN name_tree_path ntp ON ntp.id = apcn.id
  LEFT OUTER JOIN name accepted_name ON apcn.name_id = accepted_name.id

  LEFT OUTER JOIN instance_note apc_comment
  JOIN instance_note_key key1
    ON key1.id = apc_comment.instance_note_key_id AND key1.name = 'APC Comment'
    ON apc_comment.instance_id = apcn.instance_id

  LEFT OUTER JOIN instance_note apc_dist
  JOIN instance_note_key key2
    ON key2.id = apc_dist.instance_note_key_id AND key2.name = 'APC Dist.'
    ON apc_dist.instance_id = apcn.instance_id

  LEFT OUTER JOIN name firstHybridParent ON n.parent_id = firstHybridParent.id AND nt.hybrid
  LEFT OUTER JOIN name secondHybridParent ON n.second_parent_id = secondHybridParent.id AND nt.hybrid

WHERE
  --   ns.nom_inval = FALSE
  --       AND ns.nom_illeg = FALSE
  --       AND ns.name <> 'orth. var.'
  --       AND ns.name <> 'isonym'
  exists(SELECT 1
         FROM instance i
         WHERE i.name_id = n.id)
  AND n.id = 112735
ORDER BY canonical_Name;
"""
    }
}
