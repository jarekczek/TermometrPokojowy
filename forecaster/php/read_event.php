<?php header('Content-Type: text/plain'); ?>
<?php

require_once('eventStorage.php');

if ($GLOBALS['_SERVER']['COMPUTERNAME'] == 'LENOVO8G') {
  $code = 'aaa';
  $last = NULL;
  $newContent = NULL;
} else {
  $code = $_GET['code'];
  $last = $_GET['last'];
  $newContent = $_GET['new_content'];
}

validateCode($code);

$stor = new eventStorage($code);

if (!is_null($last)) {
  $lastEvent = $stor->readLast();
  echo $lastEvent;
} else {
  if (!is_null($newContent)) {
    $lastEvent = $stor->readLast();
    while (true) {
      sleep(1);
      $currentEvent = $stor->readLast();
      if ($lastEvent !== $currentEvent) {
        echo $currentEvent;
        break;
      }
    }
  } else {
    $stor->waitForAnyEvent();
    $lastEvent = $stor->readLast();
    echo $lastEvent;
  }
}

?>

