-- MySQL dump 10.13  Distrib 5.7.29-32, for debian-linux-gnu (x86_64)
--
-- Host: vip-mysql    Database: truehardcore
-- ------------------------------------------------------
-- Server version	5.7.29-32-log
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `accounts`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `accounts` (
  `id` char(36) NOT NULL,
  `playername` varchar(30) NOT NULL,
  `type` varchar(30) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `players`
--

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

--
-- Table structure for table `tracking`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tracking` (
  `id` char(36) NOT NULL,
  `ip` varchar(15) NOT NULL,
  `playername` varchar(30) NOT NULL,
  `lastseen` datetime NOT NULL,
  `firstseen` datetime NOT NULL,
  PRIMARY KEY (`id`,`ip`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `whitelist`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `whitelist` (
  `id` char(36) NOT NULL,
  `worlds` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2021-03-07 16:53:56
