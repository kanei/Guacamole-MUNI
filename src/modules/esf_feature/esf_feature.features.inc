<?php
/**
 * @file
 * esf_feature.features.inc
 */

/**
 * Implements hook_ctools_plugin_api().
 */
function esf_feature_ctools_plugin_api($module = NULL, $api = NULL) {
  if ($module == "context" && $api == "context") {
    return array("version" => "3");
  }
  if ($module == "quicktabs" && $api == "quicktabs") {
    return array("version" => "1");
  }
}

/**
 * Implements hook_views_api().
 */
function esf_feature_views_api($module = NULL, $api = NULL) {
  return array("api" => "3.0");
}

/**
 * Implements hook_eck_entity_type_info().
 */
function esf_feature_eck_entity_type_info() {
  $items = array(
    'item' => array(
      'name' => 'item',
      'label' => 'Položka',
      'properties' => array(
        'title' => array(
          'label' => 'Title',
          'type' => 'text',
          'behavior' => 'title',
        ),
        'created' => array(
          'label' => 'Created',
          'type' => 'integer',
          'behavior' => 'created',
        ),
        'changed' => array(
          'label' => 'Changed',
          'type' => 'integer',
          'behavior' => 'changed',
        ),
        'language' => array(
          'label' => 'Entity language',
          'type' => 'language',
          'behavior' => 'language',
        ),
      ),
    ),
    'person' => array(
      'name' => 'person',
      'label' => 'Osoba',
      'properties' => array(
        'created' => array(
          'label' => 'Created',
          'type' => 'integer',
          'behavior' => 'created',
        ),
        'changed' => array(
          'label' => 'Changed',
          'type' => 'integer',
          'behavior' => 'changed',
        ),
        'full_name' => array(
          'label' => 'Jméno a příjmení',
          'type' => 'text',
          'behavior' => 'title',
        ),
      ),
    ),
  );
  return $items;
}

/**
 * Implements hook_node_info().
 */
function esf_feature_node_info() {
  $items = array(
    'legal_subject' => array(
      'name' => t('Obor práva'),
      'base' => 'node_content',
      'description' => '',
      'has_title' => '1',
      'title_label' => t('Nadpis'),
      'help' => '',
    ),
    'news' => array(
      'name' => t('Novinka'),
      'base' => 'node_content',
      'description' => '',
      'has_title' => '1',
      'title_label' => t('Nadpis'),
      'help' => '',
    ),
    'page' => array(
      'name' => t('Stránka'),
      'base' => 'node_content',
      'description' => '',
      'has_title' => '1',
      'title_label' => t('Nadpis'),
      'help' => '',
    ),
    'school_subject' => array(
      'name' => t('Předmět'),
      'base' => 'node_content',
      'description' => '',
      'has_title' => '1',
      'title_label' => t('Nadpis'),
      'help' => '',
    ),
    'study_text' => array(
      'name' => t('Studijní text'),
      'base' => 'node_content',
      'description' => '',
      'has_title' => '1',
      'title_label' => t('Title'),
      'help' => '',
    ),
  );
  drupal_alter('node_info', $items);
  return $items;
}

/**
 * Implements hook_weight_features_default_settings().
 */
function esf_feature_weight_features_default_settings() {
  $settings = array();

  $settings['legal_subject'] = array(
    'enabled' => 1,
    'range' => 50,
    'menu_weight' => 0,
    'default' => 0,
    'sync_translations' => 1,
  );

  return $settings;
}
