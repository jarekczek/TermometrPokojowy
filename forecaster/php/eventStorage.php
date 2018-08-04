<?php 

class eventStorage {
  private $code;
  private $eventsDataDir;
  private $fileName;
  private $f;

  function __construct($codeArg) {
    $this->eventsDataDir = '/home/bonsoft/domains/jarek.katowice.pl/data/events';
    if ($GLOBALS['_SERVER']['COMPUTERNAME'] == 'LENOVO8G')
      $this->eventsDataDir = 'event_data/';
    date_default_timezone_set('Europe/Warsaw');
    $this->code = $codeArg;
    $today = new DateTime();
    $this->fileName = $this->eventsDataDir.'/'.$this->code.'_'.$today->format("Ymd").'.events';
  }

  function __destruct() {
  }

  function write($data) {
    $f = fopen($this->fileName, 'a+');
    fwrite($f, $data);
    fwrite($f, "\n\n");
    fclose($f);
  }

  function readLastForDate($date) {
    $filename = $this->weatherDataDir . $date->format("Ymd") . ".txt";
    $event = NULL;

    try {
      $f = fopen($this->fileName, "r");
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

  function readLast() {
    $lastEvent = $this->readLastForDate(new DateTime());
    if (is_null($lastEvent)) {
      $yesterday = (new DateTime())->sub(DateInterval::createFromDateString('1 day'));
      $lastEvent = $this->readLastForDate($yesterday);
    }
    return $lastEvent;
  }
}

function validateCode($code) {
  if ($code == '') {
    http_response_code(400);
    echo 'code required';
    die();
  }

  if (preg_match('/^[0-9a-z_]+$/', $code) === 0) {
    http_response_code(400);
    echo 'code must consist of letters, digits and underscore only.';
    die();
  }
}

?>
