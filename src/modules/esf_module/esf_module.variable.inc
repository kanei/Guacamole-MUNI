<?php


/**
 * @file
 * Variable integration for ESF module.
 */

/**
 * Implements hook_variable_info().
 */
function esf_module_variable_info($options) {

  $variables['guacamole_server_url'] = array(
    'type' => 'string',
    'title' => t('Guacamole TomCat JAVA servlet URL', array(), $options),
    'description' => t('URL of the server running the remote desktop Guacamole software.'),
    'default' => 'FILL IN',
    'localize' => FALSE,
    'group' => 'esf',
  );

  $variables['guacamole_server_port'] = array(
    'type' => 'number',
    'title' => t('Guacamole TomCat servlet port', array(), $options),
    'description' => t('Port of the server running the remote desktop Guacamole software.'),
    'default' => 0,
    'localize' => FALSE,
    'group' => 'esf',
  );
  
  $variables['aspi_protocol'] = array(
    'type' => 'string',
    'title' => t('ASPI protocol'),
    'description' => t('The URL protocol prefix ([protocol]://) used in the links to ASPI'),
    'default' => 'aspi',
    'localize' => FALSE,
    'group' => 'esf',
  );
  
  return $variables;
}

/**
 * Implements hook_variable_group_info().
 */
function esf_module_variable_group_info() {

  $groups['esf'] = array(
    'title' => t('ESF settings'),
    'description' => t('Variables for the ESF Portal configuration.'),
    'access' => 'administer esf',
    'path' => array('admin/config/esf/settings'),
  );

  return $groups;
}