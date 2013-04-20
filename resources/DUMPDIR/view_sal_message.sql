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
-- Final view structure for view `view_sal_message`
--

/*!50001 DROP TABLE IF EXISTS `view_sal_message`*/;
/*!50001 DROP VIEW IF EXISTS `view_sal_message`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8 */;
/*!50001 SET character_set_results     = utf8 */;
/*!50001 SET collation_connection      = utf8_general_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `view_sal_message` AS (select `sal_message_fact`.`message_id` AS `message_id`,`sal_message_fact`.`message_time` AS `message_time`,`sal_message_fact`.`message_text` AS `message_text`,`sal_status_dim`.`sal_status_name` AS `sal_status_name`,`sal_category_dim`.`sal_category_name` AS `sal_category_name`,`sal_user_dim`.`sal_user_name` AS `sal_user_name`,`sal_user_dim`.`sal_user_oid` AS `sal_user_oid` from (((`sal_message_fact` join `sal_user_dim` on((`sal_message_fact`.`sal_user_key` = `sal_user_dim`.`sal_user_key`))) join `sal_status_dim` on((`sal_message_fact`.`sal_status_key` = `sal_status_dim`.`sal_status_key`))) join `sal_category_dim` on((`sal_message_fact`.`sal_category_key` = `sal_category_dim`.`sal_category_key`)))) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2013-04-20 14:27:43
