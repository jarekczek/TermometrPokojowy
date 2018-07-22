<?php header('Content-Type: text/plain'); ?>
<?php
$delay = 1;
for  ($i = 1; $i <= 10; $i++) {
  echo "delay = " . $delay . " seconds." . "\n";
  flush();
  ob_flush();
  sleep($delay);
  $delay = 10 * $delay;
}
?>
ok
