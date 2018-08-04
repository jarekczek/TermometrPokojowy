<?php header('Content-Type: text/plain'); ?>
<?php

require_once('eventStorage.php');

if ($GLOBALS['_SERVER']['COMPUTERNAME'] == 'LENOVO8G') {
  $code = 'aaa';
  $last = NULL;
} else {
  $code = $_GET['code'];
  $last = $_GET['last'];
}

validateCode($code);

$stor = new eventStorage($code);
$lastEvent = $stor->readLast();

if (!is_null($last)) {
  echo $lastEvent;
} else {
  while (true) {
    sleep(1);
    $currentEvent = $stor->readLast();
    if ($lastEvent !== $currentEvent) {
      echo $currentEvent;
      break;
    }
  }
}

?>

