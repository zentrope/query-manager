-- MySQL dump 10.13  Distrib 5.1.49, for Win64 (unknown)
--
-- Host: localhost    Database: te
-- ------------------------------------------------------
-- Server version	5.1.49-enterprise-commercial-pro

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `promtch`
--

DROP TABLE IF EXISTS `promtch`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `promtch` (
  `F_SETID` bigint(20) NOT NULL,
  `F_SUBID` bigint(20) NOT NULL,
  `F_ID` bigint(20) NOT NULL,
  `F_ENID` bigint(20) NOT NULL,
  `F_ROID` bigint(20) NOT NULL,
  `F_ENCASE` tinyint(1) NOT NULL DEFAULT '0',
  `F_NAME` varchar(1536) COLLATE utf8_bin DEFAULT NULL,
  `F_TYPE` smallint(6) NOT NULL,
  `F_MD5` varbinary(16) DEFAULT NULL,
  `F_SHA1` varbinary(20) DEFAULT NULL,
  `F_SHA256` varbinary(32) DEFAULT NULL,
  `F_SHA512` varbinary(64) DEFAULT NULL,
  PRIMARY KEY (`F_SETID`,`F_SUBID`,`F_ID`),
  KEY `PROMTCH_3_ASC` (`F_ENID`),
  KEY `PROMTCH_4_ASC` (`F_ROID`),
  KEY `PROMTCH_8_ASC` (`F_MD5`),
  KEY `PROMTCH_9_ASC` (`F_SHA1`),
  KEY `PROMTCH_10_ASC` (`F_SHA256`),
  KEY `PROMTCH_11_ASC` (`F_SHA512`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2013-04-20 14:27:41
