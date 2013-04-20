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
-- Table structure for table `vers`
--

DROP TABLE IF EXISTS `vers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `vers` (
  `oid` bigint(20) NOT NULL,
  `V_EID` bigint(20) NOT NULL,
  `V_TYPE` smallint(6) NOT NULL,
  `V_SEV` smallint(6) NOT NULL,
  `V_TIME` datetime NOT NULL,
  `V_DESC` varchar(1024) COLLATE utf8_bin DEFAULT '',
  `V_ATTR` bigint(20) NOT NULL,
  `V_DATA` bigint(20) NOT NULL,
  `V_APPID` varchar(32) COLLATE utf8_bin DEFAULT NULL,
  `V_NO` int(11) NOT NULL DEFAULT '0',
  `V_PKG_HASH` int(11) DEFAULT NULL,
  `V_OUT_WIN` tinyint(1) NOT NULL DEFAULT '0',
  `V_EXISTS` tinyint(1) NOT NULL DEFAULT '0',
  `V_PROMO_ID` bigint(20) NOT NULL DEFAULT '9223372036854775807',
  PRIMARY KEY (`oid`),
  KEY `Vers_1_ASC` (`V_EID`),
  KEY `Vers_4_ASC` (`V_TIME`),
  KEY `Vers_6_ASC` (`V_ATTR`),
  KEY `Vers_7_ASC` (`V_DATA`),
  KEY `Vers_14_ASC` (`V_TYPE`,`V_EID`),
  KEY `Vers_15_ASC` (`V_PROMO_ID`),
  KEY `Vers_16_ASC` (`V_EID`,`V_TIME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2013-04-20 14:27:43
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8 */ ;
/*!50003 SET character_set_results = utf8 */ ;
/*!50003 SET collation_connection  = utf8_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER VERS_DELETEON AFTER DELETE ON Vers FOR EACH ROW
BEGIN
  DELETE FROM CVLnks_Vers WHERE pId=OLD.oid;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
