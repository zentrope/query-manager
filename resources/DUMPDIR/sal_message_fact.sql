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
-- Table structure for table `sal_message_fact`
--

DROP TABLE IF EXISTS `sal_message_fact`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sal_message_fact` (
  `message_id` bigint(20) NOT NULL,
  `message_time` datetime NOT NULL,
  `sal_status_key` int(11) NOT NULL,
  `sal_category_key` int(11) NOT NULL,
  `sal_user_key` int(11) NOT NULL,
  `message_text` longtext COLLATE utf8_bin NOT NULL,
  PRIMARY KEY (`message_id`),
  KEY `IND_sal_message_fact_time` (`message_time`),
  KEY `IND_sal_message_fact_level` (`sal_status_key`),
  KEY `IND_sal_message_fact_category` (`sal_category_key`),
  KEY `IND_sal_message_fact_user` (`sal_user_key`),
  CONSTRAINT `FK_sal_msg_fact_category` FOREIGN KEY (`sal_category_key`) REFERENCES `sal_category_dim` (`sal_category_key`),
  CONSTRAINT `FK_sal_msg_fact_status` FOREIGN KEY (`sal_status_key`) REFERENCES `sal_status_dim` (`sal_status_key`),
  CONSTRAINT `FK_sal_msg_fact_user` FOREIGN KEY (`sal_user_key`) REFERENCES `sal_user_dim` (`sal_user_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2013-04-20 14:27:42
