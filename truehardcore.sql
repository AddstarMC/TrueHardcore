-- MySQL dump 10.13  Distrib 5.5.32, for Linux (x86_64)
--
-- Host: localhost    Database: truehardcore
-- ------------------------------------------------------
-- Server version	5.5.32

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `players`
--

DROP TABLE IF EXISTS `players`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `players` (
  `id` char(36) NOT NULL,
  `player` varchar(20) NOT NULL,
  `world` varchar(20) NOT NULL,
  `spawnpos` varchar(200) DEFAULT NULL,
  `lastpos` varchar(200) DEFAULT NULL,
  `lastjoin` datetime DEFAULT NULL,
  `lastquit` datetime DEFAULT NULL,
  `gamestart` datetime DEFAULT NULL,
  `gameend` datetime DEFAULT NULL,
  `gametime` int(11) DEFAULT NULL,
  `level` int(11) DEFAULT NULL,
  `exp` int(11) DEFAULT NULL,
  `score` mediumint(9) DEFAULT NULL,
  `topscore` mediumint(9) DEFAULT NULL,
  `state` enum('NOT_IN_GAME','IN_GAME','ALIVE','DEAD') DEFAULT NULL,
  `deathmsg` varchar(255) DEFAULT NULL,
  `deathpos` varchar(100) DEFAULT NULL,
  `deaths` int(11) NOT NULL,
  `cowkills` int(11) NOT NULL,
  `pigkills` int(11) NOT NULL,
  `sheepkills` int(11) NOT NULL,
  `chickenkills` int(11) NOT NULL,
  `creeperkills` int(11) NOT NULL,
  `zombiekills` int(11) NOT NULL,
  `skeletonkills` int(11) NOT NULL,
  `spiderkills` int(11) NOT NULL,
  `enderkills` int(11) NOT NULL,
  `slimekills` int(11) NOT NULL,
  `otherkills` int(11) NOT NULL,
  `playerkills` int(11) NOT NULL,
  `mooshkills` int(11) NOT NULL,
  PRIMARY KEY (`id`,`world`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

DROP TABLE IF EXISTS `whitelist`;

CREATE TABLE `whitelist` (
  `id` char(36) NOT NULL PRIMARY KEY,
  `worlds` varchar(255) DEFAULT NULL
);
--
-- Dumping data for table `players`
--

LOCK TABLES `players` WRITE;
/*!40000 ALTER TABLE `players` DISABLE KEYS */;
/*!40000 ALTER TABLE `players` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2013-09-14 12:57:49
