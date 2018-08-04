<?php header('Content-Type: text/plain'); ?>
<?php

require_once('eventStorage.php');

if ($GLOBALS['_SERVER']['COMPUTERNAME'] == 'LENOVO8G') {
  $code = 'aaa';
  $data = 'cccc';
} else {
  $code = $_GET['code'];
  $data = $_GET['data'];
}

if ($data == '')
  $data = '<no data>';

validateCode($code);

$writer = new eventStorage($code);
$writer->write($data);

?>
