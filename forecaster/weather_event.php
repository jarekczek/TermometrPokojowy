<?php header('Content-Type: text/plain'); ?>
<?php

//error_reporting(E_STRICT | E_ALL);
//ini_set('display_errors', 1);

date_default_timezone_set('Europe/Warsaw');
$weatherDataDir = '/home/bonsoft/bin/weather_data/';
if ($GLOBALS['_SERVER']['COMPUTERNAME'] == 'LENOVO8G')
  $weatherDataDir = 'weather_data/';

function lastMsgForDate($date) {
  global $weatherDataDir;

  $filename = $weatherDataDir . $date->format("Ymd") . ".txt";
  $event = NULL;

  try {
    $f = fopen($filename, "r");
    if ($f === false)
      return NULL;
    while (true) {
      $line = fgets($f);
      if ($line === FALSE)
        break;
      $line = str_replace("\n", "", $line);
      $line = str_replace("\r", "", $line);
      if ($line !== "")
        $event = $line;
    }
    fclose($f);
  } catch (Exception $e) {
    return "error";
  }
  return $event;
}

function lastMsg() {
  $lastEvent = lastMsgForDate(new DateTime());
  if (is_null($lastEvent)) {
    $yesterday = (new DateTime())->sub(DateInterval::createFromDateString('1 day'));
    $lastEvent = lastMsgForDate($yesterday);
  }
  return $lastEvent;
}

$lastEvent = lastMsg();

if (!is_null($_GET['last'])) {
  echo $lastEvent;
} else {
  while (true) {
    sleep(10);
    $currentEvent = lastMsg();
    if ($lastEvent !== $currentEvent) {
      echo $currentEvent;
      break;
    }
  }
}

/*
  flush();
  ob_flush();
  sleep($delay);
*/

?>

